package dev.kaede.pixelboard;

import android.content.Context;
import android.app.Application;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import android.os.Build;
import android.content.pm.PackageInfo;
import android.util.Log;
import java.lang.reflect.Method;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;

/** API 102 entry. Basic cloud mode is the default; V2 backends require explicit opt-in. */
public final class PixelBoardModule extends XposedModule {
    private static final String TAG = "PixelBoardLS";
    private static final String GBOARD = "com.google.android.inputmethod.latin";

    private final AtomicBoolean bootstrapped = new AtomicBoolean(false);
    private final BackendStartup backendStartup = new BackendStartup();
    private final java.util.Set<String> writingFlagsLogged = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final AtomicInteger writingToolbarReadCount = new AtomicInteger(0);
    private volatile boolean verifiedWritingTools184;
    private volatile boolean verifiedV2;
    private volatile boolean v2CanAttempt;
    private volatile Context hostContext;

    @Override public void onModuleLoaded(io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam param) {
        Log.i(TAG, "PixelBoard API102 entry loaded");
        log(Log.INFO, TAG, "PixelBoard API102 entry loaded; module=" + BuildConfig.VERSION_NAME);
    }

    @Override public void onPackageReady(PackageReadyParam param) {
        if ("com.google.android.aicore".equals(param.getPackageName())
                || "com.google.android.as.oss".equals(param.getPackageName())) {
            prepareExperimentalBackend(param.getPackageName(), param.getClassLoader());
            return;
        }
        if (!GBOARD.equals(param.getPackageName())) return;
        ClassLoader loader = param.getClassLoader();
        if (loader == null) { Log.w(TAG, "No Gboard classloader"); return; }
        Log.i(TAG, "Gboard package ready; waiting for Application.attach");
        log(Log.INFO, TAG, "Gboard package ready; attach initialization armed");
        try {
            Method attach = Application.class.getDeclaredMethod("attach", Context.class);
            hook(attach).intercept(chain -> {
                Object result = chain.proceed();
                Object arg = chain.getArgs().isEmpty() ? null : chain.getArgs().get(0);
                if (arg instanceof Context && bootstrapped.compareAndSet(false, true)) {
                    Context appContext = (Context) arg;
                    if (GBOARD.equals(appContext.getPackageName())) {
                        try { initializeForGboard(loader, appContext); }
                        catch (Throwable ex) {
                            recordStatus(appContext, "INIT_FAILED", ex.getClass().getName());
                            Log.e(TAG, "Gboard post-attach initialization failed", ex);
                        }
                    } else {
                        bootstrapped.set(false);
                    }
                }
                return result;
            });
            Log.i(TAG, "Application.attach hook installed");
            // PackageReady can also run after an application already exists. Use the same
            // once-only initialization guard instead of waiting for a missed attach callback.
            Context existing = GboardContext.current();
            if (existing != null && bootstrapped.compareAndSet(false, true)) {
                try { initializeForGboard(loader, existing); }
                catch (Throwable ex) {
                    recordStatus(existing, "INIT_FAILED", ex.getClass().getName());
                    Log.e(TAG, "Gboard existing-context initialization failed", ex);
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
            Log.e(TAG, "Application.attach hook installation failed", ex);
        }
    }





    private void recordStatus(Context context, String event, String detail) {
        RuntimeStatus.send(context, event, detail);
        try { log(Log.INFO, TAG, event + " " + RuntimeStatusFormat.metadata(detail)); }
        catch (RuntimeException | LinkageError ignored) { }
    }

    private boolean resolveV2Profile(ClassLoader loader) {
        try {
            new TargetMethod("oli", "b", "omd").resolve(loader);
            new TargetMethod("ojq", "l", "xin", "java.lang.String", "int").resolve(loader);
            new TargetMethod("oju", "h", "oju", "android.content.Context", "pxf").resolve(loader);
            new TargetMethod("xin", "b", "void", "java.lang.Runnable",
                    "java.util.concurrent.Executor").resolve(loader);
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            if (FeatureConfig.writingToolsV2()) Log.w(TAG, "V2 profile rejected", e);
            return false;
        }
    }

    private void prepareExperimentalBackend(String packageName, ClassLoader loader) {
        if (loader == null) {
            log(Log.WARN, TAG, "Backend package ready without classloader: " + packageName);
            return;
        }
        log(Log.INFO, TAG, "Backend package ready: " + packageName + " module=" + BuildConfig.VERSION_NAME);
        try {
            hook(Application.class.getDeclaredMethod("attach", Context.class)).intercept(chain -> {
                Object result = chain.proceed();
                Object arg = chain.getArgs().isEmpty() ? null : chain.getArgs().get(0);
                if (arg instanceof Context)
                    initializeBackendOnce(packageName, loader, (Context) arg, "attach");
                return result;
            });
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            log(Log.WARN, TAG, "Backend attach observer unavailable: " + e.getClass().getSimpleName());
        }
        try {
            hook(Application.class.getDeclaredMethod("onCreate")).intercept(chain -> {
                Object result = chain.proceed();
                Context context = GboardContext.current(packageName);
                if (context != null) initializeBackendOnce(packageName, loader, context, "onCreate");
                return result;
            });
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            log(Log.WARN, TAG, "Backend onCreate observer unavailable: " + e.getClass().getSimpleName());
        }
        Context existing = GboardContext.current(packageName);
        if (existing != null) initializeBackendOnce(packageName, loader, existing, "existing");
    }

    private void initializeBackendOnce(String packageName, ClassLoader loader, Context context, String source) {
        backendStartup.start(packageName, context.getPackageName(), () -> {
            recordStatus(context, "BACKEND_PACKAGE", "source=" + source
                    + " process=" + Application.getProcessName());
            RuntimeProbe.installBackend(context);
            try {
                FeatureConfig.initialize(this);
                boolean requested = FeatureConfig.writingToolsV2();
                recordStatus(context, "MODE", "backendProcess=true v2Requested=" + requested
                        + " backend=" + FeatureConfig.writingToolsBackend());
                // Read-only observations must not depend on a package visibility/binding preflight.
                if (requested && "com.google.android.as.oss".equals(packageName))
                    observeAstreaAttestation(loader, context);
                WritingToolsBackendCheck.Result prerequisites = requested
                        ? WritingToolsBackendCheck.check(context, FeatureConfig.writingToolsBackend()) : null;
                if (BackendPreparationPolicy.independentPcsPreparation(packageName,
                        FeatureConfig.localModelPreparation())
                        || (FeatureConfig.localModelPreparation()
                            && "com.google.android.aicore".equals(packageName))) {
                    // Local Prompt model delivery does not depend on Gboard binding or V2 routing.
                    recordStatus(context, "LOCAL_MODEL_PREPARATION", "requested=true v2=" + requested);
                    try { installExperimentalBackend(packageName, loader, context); }
                    catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                        recordStatus(context, "LOCAL_MODEL_PREPARATION_FAILED", e.getClass().getSimpleName());
                        Log.w(TAG, "Independent PCS download configuration failed", e);
                    }
                } else if (prerequisites != null && prerequisites.canAttempt) {
                    try { installExperimentalBackend(packageName, loader, context); }
                    catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                        recordStatus(context, "V2_DIAGNOSTICS_FAILED", "backend_install="
                                + e.getClass().getSimpleName());
                        Log.w(TAG, "V2 backend package hook skipped: " + packageName, e);
                    }
                } else {
                    recordStatus(context, "MODE", "backendOverrides=stock reason="
                            + (prerequisites == null ? "V2_DISABLED" : prerequisites.code));
                }
            } catch (RuntimeException | LinkageError e) {
                recordStatus(context, "INIT_FAILED", "backend=" + e.getClass().getSimpleName());
                Log.e(TAG, "Backend initialization failed: " + packageName, e);
            }
        });
    }

    private void installExperimentalBackend(String packageName, ClassLoader loader, Context context)
            throws ReflectiveOperationException {
        PackageInfo pkg;
        try { pkg = context.getPackageManager().getPackageInfo(packageName, 0); }
        catch (android.content.pm.PackageManager.NameNotFoundException e) { return; }
        if ("com.google.android.as.oss".equals(packageName)) {
            observePcsManifest149829(loader, context, pkg.getLongVersionCode());
            AstreaFlagResolver.Binding binding;
            try { binding = AstreaFlagResolver.resolve(context, loader); }
            catch (Exception | LinkageError e) {
                recordStatus(context, "V2_PROFILE_FAILED", "Astrea semantic resolver="
                        + e.getClass().getSimpleName());
                Log.w(TAG, "Astrea semantic binding rejected; stock behavior retained", e);
                return;
            }
            AtomicBoolean logged = new AtomicBoolean();
            hook(binding.method).intercept(chain -> {
                if (chain.getArgs().size() == 1
                        && "ProtectedDownload__enabled".equals(chain.getArgs().get(0))) {
                    if (logged.compareAndSet(false, true))
                        recordStatus(context, "ASTREA_FLAG", "ProtectedDownload__enabled=true resolver="
                                + binding.source);
                    return "true";
                }
                return chain.proceed();
            });
            recordStatus(context, "V2_DIAGNOSTICS_READY", "Astrea semantic resolver="
                    + binding.source + " target=" + binding.method.getDeclaringClass().getName()
                    + "." + binding.method.getName() + " version=" + pkg.getLongVersionCode());
        } else {
            AicoreManifestProfile profile = AicoreManifestProfile.forPackage(
                    pkg.getLongVersionCode(), pkg.versionName);
            if (profile == null) {
                recordStatus(context, "MANIFEST_OBSERVER_UNSUPPORTED",
                        "version=" + pkg.getLongVersionCode());
                return;
            }
            if(pkg.getLongVersionCode()==513248L) observeManifestWire513248(loader,context);
            Method clientName = new TargetMethod(profile.clientClass, "a", "java.lang.String").resolve(loader);
            Method fetch = new TargetMethod(profile.fetchClass, "a", "java.lang.Object",
                    profile.clientClass, "java.util.Map", profile.continuationClass).resolve(loader);
            AtomicInteger hits = new AtomicInteger();
            hook(fetch).intercept(chain -> {
                try {
                    if (chain.getArgs().size() == 3
                            && chain.getArgs().get(1) instanceof java.util.Map
                            && "com.google.android.aicore:18103149225492435673"
                                    .equals(clientName.invoke(chain.getArgs().get(0)))
                            && hits.incrementAndGet() <= 4) {
                        java.util.Map<?, ?> map = (java.util.Map<?, ?>) chain.getArgs().get(1);
                        String line = AicoreManifestProfile.describe(map);
                        Log.i(TAG, line);
                        recordStatus(context, "MANIFEST_REQUEST", line);
                    }
                } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                    Log.w(TAG, "V2 manifest observation failed; original request retained");
                }
                return chain.proceed();
            });
            recordStatus(context, "MANIFEST_OBSERVER_READY", "version=" + pkg.getLongVersionCode()
                    + " target=" + profile.fetchClass + ".a read_only=true");
        }
    }

    private void observePcsManifest149829(ClassLoader loader, Context context, long version) {
        if (version != 149829L) {
            recordStatus(context, "MANIFEST_OBSERVER_UNSUPPORTED", "stage=pcs_remote version=" + version);
            return;
        }
        try {
            Class<?> request = Class.forName("hec", false, loader);
            PcsManifestMetadata.Binding metadata = new PcsManifestMetadata.Binding(request,
                    Class.forName("heh", false, loader), Class.forName("heg", false, loader),
                    Class.forName("hea", false, loader), Class.forName("hef", false, loader),
                    Class.forName("lqp", false, loader), Class.forName("heb", false, loader),
                    Class.forName("hei", false, loader));
            PcsRpcErrorMetadata.Binding errors = new PcsRpcErrorMetadata.Binding(
                    Class.forName("mhu", false, loader), Class.forName("mhs", false, loader),
                    Class.forName("mhp", false, loader), Class.forName("mfz", false, loader));
            PcsStatusDetails.TrailersBinding details = new PcsStatusDetails.TrailersBinding(
                    Class.forName("mfz", false, loader));
            Class<?> futureClass = Class.forName("kmw", false, loader);
            Method listener = futureClass.getMethod("c", Runnable.class, java.util.concurrent.Executor.class);
            Method rpc = Class.forName("mzd", false, loader).getDeclaredMethod("a",
                    Class.forName("mcl", false, loader), Object.class);
            if (!java.lang.reflect.Modifier.isStatic(rpc.getModifiers())
                    || rpc.getReturnType() != futureClass
                    || !java.util.concurrent.Future.class.isAssignableFrom(futureClass)
                    || listener.getReturnType() != void.class)
                throw new NoSuchMethodException("PCS RPC signature mismatch");
            rpc.setAccessible(true); listener.setAccessible(true);
            AtomicInteger captures = new AtomicInteger();
            hook(rpc).intercept(chain -> {
                int observedId = 0;
                try {
                    if (captures.get() < 16 && chain.getArgs().size() == 2
                            && request.isInstance(chain.getArgs().get(1))) {
                        String line = metadata.describe(chain.getArgs().get(1));
                        if (line != null) {
                            observedId = captures.incrementAndGet();
                            if (observedId <= 16) {
                                Log.i(TAG, "PCS_MANIFEST_WIRE id=" + observedId + " " + line);
                                recordStatus(context, "PCS_MANIFEST_WIRE", "id=" + observedId + " " + line);
                            } else observedId = 0;
                        }
                    }
                } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                    Log.w(TAG, "PCS manifest metadata unavailable; original request retained");
                }
                // Always invoke the real RPC exactly once. Do not wrap its exceptions.
                Object result = chain.proceed();
                final int requestId = observedId;
                if (requestId != 0 && result instanceof java.util.concurrent.Future) {
                    java.util.concurrent.Future<?> future = (java.util.concurrent.Future<?>) result;
                    Runnable completion = () -> {
                        if (!future.isDone()) return;
                        String line;
                        String detailLine = null;
                        try {
                            future.get(); // Listener runs only on completion; response body is never inspected.
                            line = "rpc=OK response_body=not_inspected";
                        } catch (java.util.concurrent.ExecutionException error) {
                            try { line = errors.describe(error.getCause()); }
                            catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                                line = "rpc=UNKNOWN error_metadata=unavailable";
                            }
                            try { detailLine = errors.describeDetails(error.getCause(), details); }
                            catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                                detailLine = "details=OBSERVER_UNAVAILABLE";
                            }
                        } catch (java.util.concurrent.CancellationException error) {
                            line = "rpc=CANCELLED";
                        } catch (InterruptedException error) {
                            Thread.currentThread().interrupt();
                            line = "rpc=UNKNOWN observer=interrupted";
                        } catch (RuntimeException | LinkageError error) {
                            line = "rpc=UNKNOWN observer=failed";
                        }
                        try {
                            Log.i(TAG, "PCS_MANIFEST_RESULT id=" + requestId + " " + line);
                            recordStatus(context, "PCS_MANIFEST_RESULT", "id=" + requestId + " " + line);
                            if (detailLine != null) {
                                Log.i(TAG, "PCS_MANIFEST_DETAILS id=" + requestId + " " + detailLine);
                                recordStatus(context, "PCS_MANIFEST_DETAILS", "id=" + requestId + " " + detailLine);
                            }
                        } catch (RuntimeException | LinkageError ignored) { }
                    };
                    try {
                        listener.invoke(result, completion, (java.util.concurrent.Executor) Runnable::run);
                    } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                        Log.w(TAG, "PCS RPC completion observer unavailable; original future retained");
                    }
                }
                return result;
            });
            String ready = "stage=pcs_remote version=149829 read_only=true deviceConfigPermission="
                    + (context.checkSelfPermission("android.permission.READ_DEVICE_CONFIG")
                       == android.content.pm.PackageManager.PERMISSION_GRANTED);
            Log.i(TAG, "MANIFEST_OBSERVER_READY " + ready);
            recordStatus(context, "MANIFEST_OBSERVER_READY", ready);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            String line = "stage=pcs_remote version=149829 error=" + error.getClass().getSimpleName();
            Log.w(TAG, "MANIFEST_OBSERVER_UNSUPPORTED " + line);
            recordStatus(context, "MANIFEST_OBSERVER_UNSUPPORTED", line);
        }
    }

    private void observeManifestWire513248(ClassLoader loader,Context context) {
        try {
            Class<?> request=Class.forName("icn",false,loader);
            Class<?> labels=Class.forName("icm",false,loader);
            ManifestRequestMetadata.Binding binding=new ManifestRequestMetadata.Binding(request,labels);
            Method rpc=Class.forName("icu",false,loader).getDeclaredMethod(
                    "b",Class.forName("krb",false,loader),request);
            if(java.lang.reflect.Modifier.isStatic(rpc.getModifiers())
                    || !java.util.concurrent.Future.class.isAssignableFrom(rpc.getReturnType()))
                throw new NoSuchMethodException("Unexpected manifest RPC signature");
            AtomicInteger hits=new AtomicInteger();
            hook(rpc).intercept(chain -> {
                try {
                    if(chain.getArgs().size()==2 && request.isInstance(chain.getArgs().get(1))
                            && hits.incrementAndGet()<=16) {
                        String line=binding.describe(chain.getArgs().get(1));
                        Log.i(TAG,"MANIFEST_WIRE "+line);
                        recordStatus(context,"MANIFEST_WIRE",line);
                    }
                } catch(ReflectiveOperationException|RuntimeException|LinkageError error) {
                    Log.w(TAG,"Manifest wire observation failed; original request retained");
                }
                return chain.proceed();
            });
            recordStatus(context,"MANIFEST_OBSERVER_READY","stage=pre_pcs_rpc version=513248 read_only=true");
        } catch(ReflectiveOperationException|RuntimeException|LinkageError error) {
            recordStatus(context,"MANIFEST_OBSERVER_UNSUPPORTED","stage=pre_pcs_rpc error="+error.getClass().getSimpleName());
            Log.w(TAG,"Manifest wire observer not installed",error);
        }
    }

    private void observeV2Backend(ClassLoader loader) {
        try {
            Method selector = new TargetMethod("oli", "b", "omd").resolve(loader);
            AtomicInteger routeHits = new AtomicInteger();
            hook(selector).intercept(chain -> {
                Object result = chain.proceed();
                if (routeHits.incrementAndGet() <= 8) {
                    Log.i(TAG, "V2 selected backend=" + enumName(result));
                    recordStatus(hostContext, "V2_ROUTE", enumName(result));
                }
                return result;
            });
            Method prepare = new TargetMethod("ojq", "l", "xin", "java.lang.String", "int").resolve(loader);
            Method listener = new TargetMethod("xin", "b", "void",
                    "java.lang.Runnable", "java.util.concurrent.Executor").resolve(loader);
            java.util.Set<Object> observed = java.util.Collections.synchronizedSet(
                    java.util.Collections.newSetFromMap(new java.util.WeakHashMap<Object, Boolean>()));
            AtomicInteger prepares = new AtomicInteger();
            hook(prepare).intercept(chain -> {
                Object result = chain.proceed();
                if (result instanceof java.util.concurrent.Future && observed.add(result)
                        && prepares.incrementAndGet() <= 8) {
                    int feature = (Integer) chain.getArgs().get(1);
                    Log.i(TAG, "V2 AICore session requested; feature=" + feature);
                    recordStatus(hostContext, "V2_SESSION_REQUEST", "feature=" + feature);
                    try {
                        java.util.concurrent.Future<?> future = (java.util.concurrent.Future<?>) result;
                        listener.invoke(result, (Runnable) () -> {
                            // Never wait, consume/close a session, or alter the original future.
                            if (!future.isDone()) return;
                            try {
                                future.get(0, java.util.concurrent.TimeUnit.MILLISECONDS);
                                Log.i(TAG, "V2 AICore session prepared; feature=" + feature);
                                recordStatus(hostContext, "V2_SESSION_READY", "feature=" + feature);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            } catch (Exception e) {
                                recordStatus(hostContext, "V2_SESSION_FAILED", "feature=" + feature + " " + errorTypes(e));
                                Log.w(TAG, "V2 AICore session failed; feature=" + feature
                                        + "; causes=" + errorTypes(e));
                            }
                        }, (java.util.concurrent.Executor) Runnable::run);
                    } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                        Log.w(TAG, "V2 future observation unavailable; original future retained");
                    }
                }
                return result;
            });
            Method build = new TargetMethod("omo", "a", "omq").resolve(loader);
            Class<?> response = Class.forName("omq", false, loader);
            java.lang.reflect.Field status = response.getDeclaredField("h");
            java.lang.reflect.Field source = response.getDeclaredField("k");
            if (!"omr".equals(status.getType().getName()) || !"omn".equals(source.getType().getName()))
                throw new NoSuchFieldException("Response metadata types changed");
            status.setAccessible(true);
            source.setAccessible(true);
            AtomicInteger responses = new AtomicInteger();
            hook(build).intercept(chain -> {
                Object result = chain.proceed();
                if (responses.incrementAndGet() <= 24 && result != null) {
                    try {
                        recordStatus(hostContext, "V2_RESPONSE", "source=" + enumName(source.get(result))
                                + " status=" + enumName(status.get(result)));
                        Log.i(TAG, "V2 response built: source=" + enumName(source.get(result))
                                + " status=" + enumName(status.get(result)));
                    } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) { }
                }
                return result;
            });
            try {
                Method panel = new TargetMethod("htm", "E", "void", "java.lang.String",
                        "java.lang.CharSequence", "htj", "java.lang.String", "boolean", "boolean",
                        "xac", "boolean", "wyb").resolve(loader);
                AtomicInteger panelEvents = new AtomicInteger();
                hook(panel).intercept(chain -> {
                    Object result = chain.proceed();
                    if (chain.getArgs().size() == 9 && panelEvents.incrementAndGet() <= 12)
                        recordStatus(hostContext, "V2_PANEL_ERROR", enumName(chain.getArgs().get(6)));
                    return result;
                });
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                recordStatus(hostContext, "V2_DIAGNOSTICS_FAILED", "PANEL_STATUS_OBSERVER_UNAVAILABLE");
            }
            observeAstreaErrors(loader);
            observeV2Availability(loader);
            recordStatus(hostContext, "V2_DIAGNOSTICS_READY", "route session response availability");
            Log.i(TAG, "V2 route/session/response observers installed; no text logged");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            recordStatus(hostContext, "V2_DIAGNOSTICS_FAILED", e.getClass().getName());
            Log.w(TAG, "V2 backend diagnostics incomplete; original backend behavior retained", e);
        }
    }




    private void observeAstreaAttestation(ClassLoader loader, Context context) {
        try {
            Class<?> callback = Class.forName(
                    "com.google.android.apps.miphone.astrea.privateinference.library.bsa.jni."
                    + "BlindSignAuthJniBridge$NativeAttestAndSignCallback", false, loader);
            Method data = callback.getDeclaredMethod("onAttestationData", byte[][].class, byte[].class);
            if (data.getReturnType() != void.class || java.lang.reflect.Modifier.isNative(data.getModifiers()))
                throw new NoSuchMethodException("Unexpected attestation callback");
            AtomicInteger captures = new AtomicInteger();
            hook(data).intercept(chain -> {
                try {
                    if (captures.incrementAndGet() <= 4 && chain.getArgs().size() == 2
                            && chain.getArgs().get(0) instanceof byte[][])
                        recordStatus(context, "ASTREA_ATTESTATION",
                                AttestationMetadata.describe((byte[][]) chain.getArgs().get(0)));
                } catch (RuntimeException | LinkageError ignored) { }
                return chain.proceed();
            });
            Method error = callback.getDeclaredMethod("onError", Throwable.class);
            AtomicInteger failures = new AtomicInteger();
            hook(error).intercept(chain -> {
                try {
                    if (failures.incrementAndGet() <= 4 && !chain.getArgs().isEmpty()
                            && chain.getArgs().get(0) instanceof Throwable)
                        recordStatus(context, "ASTREA_ATTESTATION",
                                "generation_callback=error "
                                + RuntimeErrorMetadata.describe((Throwable) chain.getArgs().get(0)));
                } catch (RuntimeException | LinkageError ignored) { }
                return chain.proceed();
            });
            recordStatus(context, "V2_DIAGNOSTICS_READY", "Astrea attestation metadata observer; read_only=true");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            recordStatus(context, "V2_DIAGNOSTICS_FAILED", "attestation_observer=" + e.getClass().getSimpleName());
        }
    }

    private void observeAstreaErrors(ClassLoader loader) {
        try {
            Method error = new TargetMethod("oju", "k", "xin",
                    "java.lang.Throwable", "java.lang.String").resolve(loader);
            Method statusFromThrowable = new TargetMethod("abdv", "c", "abdv",
                    "java.lang.Throwable").resolve(loader);
            java.lang.reflect.Field code = Class.forName("abdv", false, loader).getDeclaredField("n");
            if (!"abds".equals(code.getType().getName())) throw new NoSuchFieldException("gRPC code");
            code.setAccessible(true);
            AtomicInteger errors = new AtomicInteger();
            hook(error).intercept(chain -> {
                // Read metadata before the original handler folds it into INTERNAL_ERROR.
                // Never log exception messages, operation strings or input/output text.
                try {
                    if (!chain.getArgs().isEmpty() && chain.getArgs().get(0) instanceof Throwable
                            && errors.incrementAndGet() <= 24) {
                        Throwable failure = (Throwable) chain.getArgs().get(0);
                        Object status = statusFromThrowable.invoke(null, failure);
                        recordStatus(hostContext, "V2_ASTREA_ERROR",
                                "rpc=" + enumName(code.get(status)) + " "
                                + RuntimeErrorMetadata.describe(failure));
                    }
                } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                    recordStatus(hostContext, "V2_DIAGNOSTICS_FAILED",
                            "astrea_error_metadata=" + e.getClass().getSimpleName());
                }
                return chain.proceed();
            });
            recordStatus(hostContext, "V2_DIAGNOSTICS_READY", "astrea_rpc_error_observer");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            recordStatus(hostContext, "V2_DIAGNOSTICS_FAILED",
                    "astrea_error_observer=" + e.getClass().getSimpleName());
        }
    }

    private void observeV2Availability(ClassLoader loader) {
        try {
            Method listener = new TargetMethod("xin", "b", "void", "java.lang.Runnable",
                    "java.util.concurrent.Executor").resolve(loader);
            for (String owner : new String[] {"ojq", "imu", "hgm"}) {
                Method check = new TargetMethod(owner, "b", "xin", "android.content.Context").resolve(loader);
                String label = "ojq".equals(owner) ? "private703" : "imu".equals(owner) ? "localProofread" : "hybrid";
                AtomicInteger checks = new AtomicInteger();
                java.util.Set<Object> observed = java.util.Collections.synchronizedSet(
                        java.util.Collections.newSetFromMap(new java.util.WeakHashMap<Object, Boolean>()));
                hook(check).intercept(chain -> {
                    Object result = chain.proceed();
                    if (result instanceof java.util.concurrent.Future && observed.add(result)
                            && checks.incrementAndGet() <= 6) {
                        recordStatus(hostContext, "V2_AVAILABILITY_REQUEST", "source=" + label);
                        java.util.concurrent.Future<?> future = (java.util.concurrent.Future<?>) result;
                        try {
                            listener.invoke(result, (Runnable) () -> {
                                if (!future.isDone()) return;
                                try {
                                    Object value = future.get(0, java.util.concurrent.TimeUnit.MILLISECONDS);
                                    recordStatus(hostContext, "V2_AVAILABILITY", "source=" + label
                                            + " status=" + enumName(value));
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                } catch (Exception e) {
                                    recordStatus(hostContext, "V2_AVAILABILITY_FAILED",
                                            "source=" + label + " " + errorTypes(e));
                                }
                            }, (java.util.concurrent.Executor) Runnable::run);
                        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                            recordStatus(hostContext, "V2_DIAGNOSTICS_FAILED", "availability_listener_" + label);
                        }
                    }
                    return result;
                });
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            recordStatus(hostContext, "V2_DIAGNOSTICS_FAILED", "availability_" + e.getClass().getSimpleName());
        }
    }

    private static String enumName(Object value) {
        return value instanceof Enum<?> ? ((Enum<?>) value).name() : "<unknown>";
    }

    private static String errorTypes(Throwable error) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; error != null && i < 6; i++) {
            if (i != 0) line.append(" -> ");
            line.append(error.getClass().getName());
            // ele.b is the inspected Gboard AICore error code; no message or request text.
            if ("ele".equals(error.getClass().getName())) {
                try {
                    java.lang.reflect.Field code = error.getClass().getDeclaredField("b");
                    if (code.getType() == int.class) {
                        code.setAccessible(true);
                        line.append("(code=").append(code.getInt(error)).append(")");
                    }
                } catch (ReflectiveOperationException | RuntimeException ignored) { }
            }
            Throwable next = error.getCause();
            if (next == error) break;
            error = next;
        }
        return line.toString();
    }

    private void initializeForGboard(ClassLoader loader, Context context) {
        hostContext = context.getApplicationContext() == null ? context : context.getApplicationContext();
        RuntimeProbe.install(hostContext, loader);
        FeatureConfig.initialize(this);
        GboardPixelIdentity.apply(context.getPackageName(), FeatureConfig.gboardPixelIdentity());
        recordStatus(context, "GBOARD_IDENTITY", GboardPixelIdentity.details());
        recordStatus(context, "GBOARD_ATTACHED", "writingTools=" + FeatureConfig.writingTools()
                + " v2Requested=" + FeatureConfig.writingToolsV2()
                + " backend=" + FeatureConfig.writingToolsBackend());
        try {
            PackageInfo pkg = context.getPackageManager().getPackageInfo(GBOARD, 0);
            verifiedWritingTools184 = pkg.getLongVersionCode() == 176027078L
                    && "18.4.1.985164140-release-arm64-v8a".equals(pkg.versionName);
            Log.i(TAG, "Target Gboard version=" + pkg.versionName
                    + ", versionCode=" + pkg.getLongVersionCode());
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Gboard package version unavailable", e);
        }
        Log.i(TAG, "Gboard Application Context available; SDK " + Build.VERSION.SDK_INT
              + "; Rambler=" + FeatureConfig.rambler()
              + "; diagnostics=" + FeatureConfig.diagnostics());
        verifiedV2 = verifiedWritingTools184 && resolveV2Profile(loader);
        v2CanAttempt = false;
        if (FeatureConfig.writingToolsV2()) {
            if (!verifiedV2) {
                Log.w(TAG, "V2 blocked=GBOARD_PROFILE_UNSUPPORTED; basic cloud policy retained");
                recordStatus(context, "V2_PROFILE_FAILED", "GBOARD_PROFILE_UNSUPPORTED");
            } else {
                WritingToolsBackendCheck.Result prerequisites = WritingToolsBackendCheck.check(
                        context, FeatureConfig.writingToolsBackend());
                v2CanAttempt = prerequisites.canAttempt;
                Log.i(TAG, "V2 binding preflight: canAttempt=" + v2CanAttempt
                        + " reason=" + prerequisites.code + "; " + prerequisites.message);
                recordStatus(context, "MODE", "canAttempt=" + v2CanAttempt
                        + " reason=" + prerequisites.code + " requestedBackend=" + FeatureConfig.writingToolsBackend());
            }
        }
        if (FeatureConfig.spellchecker()) installSpellChecker(loader);
        if (FeatureConfig.writingTools()) {
            WritingToolsDiagnostics.probe(loader);
            if ((FeatureConfig.diagnostics() || FeatureConfig.writingToolsV2()) && verifiedWritingTools184)
                observeWritingTools184State(loader);
            if (v2CanAttempt) observeV2Backend(loader);
        }
        RamblerBindings.Profile voiceProfile = null;
        if (FeatureConfig.rambler() || FeatureConfig.diagnostics()) {
            voiceProfile = RamblerBindings.select(loader);
            if (voiceProfile != null) installRambler(loader, voiceProfile);
            else Log.w(TAG, "No unique verified Rambler selector signatures; voice hooks skipped");
        }
        if (FeatureConfig.rambler() || FeatureConfig.writingTools()) {
            RamblerFlagBindings.Binding flags = voiceProfile != null
                    ? RamblerFlagBindings.resolve(loader, voiceProfile.id)
                    : RamblerFlagBindings.resolveUnique(loader);
            if (flags != null) installFlagGetter(flags);
            else {
                Log.w(TAG, "No unique typed flag getter; feature overrides skipped");
                recordStatus(context, "FLAGS_FAILED", "NO_UNIQUE_FLAG_GETTER");
            }
        }
        if (FeatureConfig.writingTools()) {
            boolean v2 = v2CanAttempt;
            Log.i(TAG, "Writing Tools policy=" + (v2 ? "v2-backends-experiment" : "basic-cloud-v2")
                    + "; V2=" + v2 + "; effectiveBackend=" + (v2 ? FeatureConfig.writingToolsBackend() : 1)
                    + "; requestedBackend=" + FeatureConfig.writingToolsBackend()
                    + "; real provisioning/consent/account checks retained");
            recordStatus(context, "MODE", "effectiveV2=" + v2 + " effectiveBackend="
                    + (v2 ? FeatureConfig.writingToolsBackend() : 1) + " requestedBackend="
                    + FeatureConfig.writingToolsBackend() + " localProofread=" + FeatureConfig.v2LocalProofread());
        }
    }

    private void installRambler(ClassLoader loader, RamblerBindings.Profile profile) {
        Log.i(TAG, "Resolved selector signature family: " + profile.id);
        hookScope(profile.layout, loader, false);
        hookScope(profile.setup, loader, false);
        hookScope(profile.create, loader, false);
        hookScope(profile.defaults, loader, true);
        observeSelectionRead(profile.read, loader);
        observeSelectionWrite(profile.write, loader);
    }

    private void hookScope(TargetMethod target, ClassLoader loader, boolean defaults) {
        try {
            hook(target.resolve(loader)).intercept(chain -> {
                if (defaults) RamblerScope.enterDefault(); else RamblerScope.enterVoice();
                try { return chain.proceed(); }
                finally {
                    if (defaults) RamblerScope.exitDefault();
                    else RamblerScope.exitVoice();
                }
            });
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            Log.w(TAG, "Scope hook skipped: " + target, e);
        }
    }

    private void observeSelectionRead(TargetMethod target, ClassLoader loader) {
        try {
            hook(target.resolve(loader)).intercept(chain -> {
                Object result = chain.proceed();
                if (result instanceof Boolean) {
                    // Use host-owned Context rather than trusting a foreign caller.
                    RamblerSelection.observe((Boolean) result, GboardContext.current());
                }
                return result;
            });
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            Log.w(TAG, "Selection read observer skipped: " + target, e);
        }
    }

    private void observeSelectionWrite(TargetMethod target, ClassLoader loader) {
        try {
            hook(target.resolve(loader)).intercept(chain -> {
                Object result = chain.proceed();
                if (FeatureConfig.diagnostics())
                    Log.d(TAG, "Official selector write completed (no forced selection)");
                return result;
            });
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            Log.w(TAG, "Selection write observer skipped: " + target, e);
        }
    }

    private void installFlagGetter(RamblerFlagBindings.Binding binding) {
        try {
            hook(binding.getter).intercept(chain -> {
                Object original = chain.proceed();
                Object receiver = chain.getThisObject();
                if (receiver == null) return original;
                try {
                    String flag = binding.name(receiver);
                    if (!RamblerFlagPolicy.targeted(flag) && !WritingToolsFlagPolicy.targeted(flag)) return original;
                    Context context = hostContext;
                    if (context == null || !GBOARD.equals(context.getPackageName()))
                        return original;
                    // Use the Gboard-owned attach context, avoiding early ActivityThread nulls.
                    Object writingResult = "writing_tools_enable_stable_entrance".equals(flag)
                            && !verifiedWritingTools184 ? original
                            : WritingToolsFlagPolicy.overrideValue(flag, original, FeatureConfig.writingTools(),
                                    v2CanAttempt, FeatureConfig.writingToolsBackend(), FeatureConfig.v2LocalProofread());
                    if (FeatureConfig.writingTools() && WritingToolsFlagPolicy.isToolbarFlag(flag)) {
                        int reads = writingToolbarReadCount.incrementAndGet();
                        if (reads <= 5)
                            Log.i(TAG, "WritingTools toolbar gate READ #" + reads
                                    + " changed=" + !java.util.Objects.equals(original, writingResult));
                    }
                    if (FeatureConfig.writingTools() && WritingToolsFlagPolicy.targeted(flag)
                            && writingFlagsLogged.add(flag)) {
                        Log.i(TAG, "WritingTools flag observed: " + flag
                                + " type=" + (original == null ? "null" : original.getClass().getSimpleName())
                                + " changed=" + !java.util.Objects.equals(original, writingResult));
                    }
                    return RamblerFlagPolicy.transform(flag, writingResult, context);
                } catch (ReflectiveOperationException | RuntimeException e) {
                    Log.w(TAG, "Flag transform failed; stock value returned", e);
                    return original;
                }
            });
            recordStatus(hostContext, "FLAGS_READY", "profile=" + binding.profile);
            Log.i(TAG, "Installed shared guarded Rambler/WritingTools flag policy: " + binding.owner
                    + " / profile=" + binding.profile);
        } catch (RuntimeException | LinkageError e) {
            recordStatus(hostContext, "FLAGS_FAILED", e.getClass().getName());
            Log.w(TAG, "Flag getter hook skipped", e);
        }
    }

    /** Exact 18.4 mapping. Read signals after stock callbacks; never alter eligibility. */
    private void observeWritingTools184State(ClassLoader loader) {
        try {
            Method signalRead = new TargetMethod("qeq", "e", "boolean", "qem").resolve(loader);
            Class<?> signals = Class.forName("qrr", false, loader);
            java.lang.reflect.Field[] fields = new java.lang.reflect.Field[5];
            String[] names = {"c", "d", "g", "h", "i"};
            String[] labels = {"proofreadProvisioned", "proofreadReady", "handlerProvisioned",
                    "handlerEverProvisioned", "handlerReady"};
            Class<?> signalType = Class.forName("qem", false, loader);
            for (int i = 0; i < fields.length; i++) {
                fields[i] = signals.getDeclaredField(names[i]);
                if (fields[i].getType() != signalType
                        || !java.lang.reflect.Modifier.isStatic(fields[i].getModifiers()))
                    throw new NoSuchFieldException("Unexpected signal type: " + names[i]);
                fields[i].setAccessible(true);
            }
            Method network = new TargetMethod("qyn", "a", "qym").resolve(loader);
            Method updater = new TargetMethod("hpz", "fM", "void",
                    "android.content.Context", "qad").resolve(loader);
            Method menu = new TargetMethod("hmp", "d", "void").resolve(loader);
            AtomicInteger hits = new AtomicInteger();
            for (Method target : new Method[] {updater, menu}) {
                hook(target).intercept(chain -> {
                    Object result = chain.proceed();
                    if (hits.incrementAndGet() <= 12) {
                        try {
                            StringBuilder line = new StringBuilder("WritingTools 18.4 state after ")
                                    .append(target.getDeclaringClass().getSimpleName());
                            for (int i = 0; i < fields.length; i++)
                                line.append(" ").append(labels[i]).append("=")
                                        .append(signalRead.invoke(null, fields[i].get(null)));
                            // qyn is NetworkInfoNotification, not the inference backend.
                            if (target == menu) {
                                Object value = network.invoke(null);
                                line.append(" network=").append(value instanceof Enum<?>
                                        ? ((Enum<?>) value).name() : "<unknown>");
                            }
                            Log.i(TAG, line.toString());
                            recordStatus(hostContext, "STATE", line.toString());
                            recordStatus(hostContext, "HANDLER_STATUS", WritingToolsHandlerSnapshot.read(loader));
                        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                            Log.w(TAG, "WritingTools state observation skipped", e);
                        }
                    }
                    return result;
                });
            }
            Log.i(TAG, "WritingTools 18.4 state observers installed; no signal overrides");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            Log.w(TAG, "WritingTools 18.4 state observers not installed", e);
        }
    }

    private void installSpellChecker(ClassLoader loader) {
        if (Build.VERSION.SDK_INT < 31) return;
        try {
            TargetMethod target = new TargetMethod("rcq", "m", "boolean",
                    "android.content.Context", "android.view.textservice.TextServicesManager");
            Method method = target.resolve(loader);
            hook(method).intercept(chain -> true);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            Log.w(TAG, "Spellchecker hook skipped", e);
        }
    }
}

