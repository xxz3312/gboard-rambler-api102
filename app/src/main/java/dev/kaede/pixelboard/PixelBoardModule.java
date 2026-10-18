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
    private final java.util.Set<String> writingFlagsLogged = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final AtomicInteger writingToolbarReadCount = new AtomicInteger(0);
    private volatile boolean verifiedWritingTools184;
    private volatile boolean verifiedV2;
    private volatile boolean v2CanAttempt;
    private volatile Context hostContext;

    @Override public void onModuleLoaded(io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam param) {
        Log.i(TAG, "PixelBoard API102 entry loaded");
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
        try {
            Method attach = Application.class.getDeclaredMethod("attach", Context.class);
            hook(attach).intercept(chain -> {
                Object result = chain.proceed();
                Object arg = chain.getArgs().isEmpty() ? null : chain.getArgs().get(0);
                if (arg instanceof Context && bootstrapped.compareAndSet(false, true)) {
                    Context appContext = (Context) arg;
                    if (GBOARD.equals(appContext.getPackageName())) {
                        try { initializeForGboard(loader, appContext); }
                        catch (Throwable ex) { Log.e(TAG, "Gboard post-attach initialization failed", ex); }
                    } else {
                        bootstrapped.set(false);
                    }
                }
                return result;
            });
            Log.i(TAG, "Application.attach hook installed");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
            Log.e(TAG, "Application.attach hook installation failed", ex);
        }
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
        if (loader == null) return;
        try {
            hook(Application.class.getDeclaredMethod("attach", Context.class)).intercept(chain -> {
                Object result = chain.proceed();
                Object arg = chain.getArgs().isEmpty() ? null : chain.getArgs().get(0);
                if (arg instanceof Context && packageName.equals(((Context) arg).getPackageName())
                        && bootstrapped.compareAndSet(false, true)) {
                    FeatureConfig.initialize(this);
                    WritingToolsBackendCheck.Result prerequisites = FeatureConfig.writingToolsV2()
                            ? WritingToolsBackendCheck.check((Context) arg, FeatureConfig.writingToolsBackend()) : null;
                    if (prerequisites != null && prerequisites.canAttempt) {
                        try { installExperimentalBackend(packageName, loader, (Context) arg); }
                        catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                            Log.w(TAG, "V2 backend package hook skipped: " + packageName, e);
                        }
                    } else Log.i(TAG, "V2 backend process retained stock behavior; reason="
                            + (prerequisites == null ? "V2_DISABLED" : prerequisites.code));
                }
                return result;
            });
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            Log.w(TAG, "V2 backend attach skipped", e);
        }
    }

    private void installExperimentalBackend(String packageName, ClassLoader loader, Context context)
            throws ReflectiveOperationException {
        PackageInfo pkg;
        try { pkg = context.getPackageManager().getPackageInfo(packageName, 0); }
        catch (android.content.pm.PackageManager.NameNotFoundException e) { return; }
        if ("com.google.android.as.oss".equals(packageName)) {
            if (pkg.getLongVersionCode() != 116015L
                    || !"1.0.release.825068975".equals(pkg.versionName)) {
                Log.w(TAG, "V2 Astrea version unsupported; stock behavior retained");
                return;
            }
            Method flag = new TargetMethod("efh", "g", "java.lang.String", "java.lang.String").resolve(loader);
            AtomicBoolean logged = new AtomicBoolean();
            hook(flag).intercept(chain -> {
                if (chain.getArgs().size() == 1
                        && "ProtectedDownload__enabled".equals(chain.getArgs().get(0))) {
                    if (logged.compareAndSet(false, true))
                        Log.i(TAG, "V2 Astrea protected download flag applied; request acceptance unverified");
                    return "true";
                }
                return chain.proceed();
            });
            Log.i(TAG, "V2 Astrea 116015 protected download hook installed");
        } else {
            if (pkg.getLongVersionCode() != 526928L
                    || !"0.release.qc8635.prod_aicore_20260917.00_RC01".equals(pkg.versionName)) {
                Log.w(TAG, "V2 AICore version unsupported; manifest observer skipped");
                return;
            }
            Method clientName = new TargetMethod("isz", "a", "java.lang.String").resolve(loader);
            Method fetch = new TargetMethod("ity", "a", "java.lang.Object", "isz", "java.util.Map", "nrj")
                    .resolve(loader);
            AtomicInteger hits = new AtomicInteger();
            hook(fetch).intercept(chain -> {
                try {
                    if (chain.getArgs().size() == 3
                            && chain.getArgs().get(1) instanceof java.util.Map
                            && "com.google.android.aicore:18103149225492435673"
                                    .equals(clientName.invoke(chain.getArgs().get(0)))
                            && hits.incrementAndGet() <= 4) {
                        java.util.Map<?, ?> map = (java.util.Map<?, ?>) chain.getArgs().get(1);
                        StringBuilder line = new StringBuilder("V2 AICore manifest labels (unchanged):");
                        for (String key : new String[] {"build_id", "device_tier", "variant", "client_group"}) {
                            Object value = map.get(key);
                            String text = value instanceof String ? (String) value : "<absent>";
                            line.append(" ").append(key).append("=").append(text.isEmpty() ? "<empty>"
                                    : text.substring(0, Math.min(96, text.length()))
                                            .replace((char) 10, ' ').replace((char) 13, ' '));
                        }
                        Log.i(TAG, line.toString());
                    }
                } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                    Log.w(TAG, "V2 manifest observation failed; original request retained");
                }
                return chain.proceed();
            });
            Log.i(TAG, "V2 AICore manifest observer installed; no label/build ID overrides");
        }
    }

    private void observeV2Backend(ClassLoader loader) {
        try {
            Method selector = new TargetMethod("oli", "b", "omd").resolve(loader);
            AtomicInteger routeHits = new AtomicInteger();
            hook(selector).intercept(chain -> {
                Object result = chain.proceed();
                if (routeHits.incrementAndGet() <= 8)
                    Log.i(TAG, "V2 selected backend=" + enumName(result));
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
                    try {
                        java.util.concurrent.Future<?> future = (java.util.concurrent.Future<?>) result;
                        listener.invoke(result, (Runnable) () -> {
                            // Never wait, consume/close a session, or alter the original future.
                            if (!future.isDone()) return;
                            try {
                                future.get(0, java.util.concurrent.TimeUnit.MILLISECONDS);
                                Log.i(TAG, "V2 AICore session prepared; feature=" + feature);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            } catch (Exception e) {
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
                        Log.i(TAG, "V2 response built: source=" + enumName(source.get(result))
                                + " status=" + enumName(status.get(result)));
                    } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) { }
                }
                return result;
            });
            Log.i(TAG, "V2 route/session/response observers installed; no text logged");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            Log.w(TAG, "V2 backend diagnostics incomplete; original backend behavior retained", e);
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
        FeatureConfig.initialize(this);
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
            } else {
                WritingToolsBackendCheck.Result prerequisites = WritingToolsBackendCheck.check(
                        context, FeatureConfig.writingToolsBackend());
                v2CanAttempt = prerequisites.canAttempt;
                Log.i(TAG, "V2 binding preflight: canAttempt=" + v2CanAttempt
                        + " reason=" + prerequisites.code + "; " + prerequisites.message);
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
            else Log.w(TAG, "No unique typed flag getter; feature overrides skipped");
        }
        if (FeatureConfig.writingTools()) {
            boolean v2 = v2CanAttempt;
            Log.i(TAG, "Writing Tools policy=" + (v2 ? "v2-backends-experiment" : "basic-cloud-v2")
                    + "; V2=" + v2 + "; effectiveBackend=" + (v2 ? FeatureConfig.writingToolsBackend() : 1)
                    + "; requestedBackend=" + FeatureConfig.writingToolsBackend()
                    + "; real provisioning/consent/account checks retained");
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
                                    v2CanAttempt, FeatureConfig.writingToolsBackend());
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
            Log.i(TAG, "Installed shared guarded Rambler/WritingTools flag policy: " + binding.owner
                    + " / profile=" + binding.profile);
        } catch (RuntimeException | LinkageError e) {
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
