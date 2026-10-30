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

/** Modern LSPosed entry scoped to Gboard; local-model experiments are inactive. */
public final class PixelBoardModule extends XposedModule {
    private static final String TAG = "PixelBoardLS";
    private static final String GBOARD = "com.google.android.inputmethod.latin";

    private final AtomicBoolean bootstrapped = new AtomicBoolean(false);
    private final java.util.Set<String> writingFlagsLogged = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final java.util.Set<String> ramblerDiagnostics = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final AtomicInteger writingToolbarReadCount = new AtomicInteger(0);
    private volatile boolean verifiedWritingTools184;
    private volatile Context hostContext;

    @Override public void onModuleLoaded(io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam param) {
        Log.i(TAG, "PixelBoard API102 entry loaded");
    }

    @Override public void onPackageReady(PackageReadyParam param) {
        if (!GBOARD.equals(param.getPackageName())) return;
        ClassLoader loader = param.getClassLoader();
        if (loader == null) { Log.w(TAG, "No Gboard classloader"); return; }
        Log.i(TAG, "Gboard package ready; waiting for Application.attach");
        try {
            Method attach = Application.class.getDeclaredMethod("attach", Context.class);
            hook(attach).intercept(chain -> {
                Object arg = chain.getArgs().isEmpty() ? null : chain.getArgs().get(0);
                if (arg instanceof Context && bootstrapped.compareAndSet(false, true)) {
                    Context appContext = (Context) arg;
                    if (GBOARD.equals(appContext.getPackageName())) {
                        try { initializeForGboard(loader, appContext); }
                        catch (Throwable ex) {
                            CloudRuntimeStatus.send(appContext,"ERROR","initialization="+ex.getClass().getSimpleName());
                            Log.e(TAG, "Gboard pre-attach initialization failed", ex);
                        }
                    } else {
                        bootstrapped.set(false);
                    }
                }
                // Application.attach invokes host attachBaseContext; install before it reads/caches flags.
                return chain.proceed();
            });
            Log.i(TAG, "Application.attach hook installed (before host attachBaseContext)");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
            Log.e(TAG, "Application.attach hook installation failed", ex);
        }
    }



    private void initializeForGboard(ClassLoader loader, Context context) {
        hostContext = context.getApplicationContext() == null ? context : context.getApplicationContext();
        FeatureConfig.initialize(this);
        FeatureConfig.onChange(() -> CloudRuntimeStatus.send(hostContext,"CONFIG_APPLIED",
                "runtimePolicy=updated; host cached UI/models may need restart"));
        try {
            PackageInfo pkg = context.getPackageManager().getPackageInfo(GBOARD, 0);
            verifiedWritingTools184 = pkg.getLongVersionCode() == 176027078L
                    && "18.4.1.985164140-release-arm64-v8a".equals(pkg.versionName);
            Log.i(TAG, "Target Gboard version=" + pkg.versionName
                    + ", versionCode=" + pkg.getLongVersionCode());
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Gboard package version unavailable", e);
        }
        GboardResolver.Result resolved=GboardResolver.resolve(hostContext,loader);
        boolean voiceCompatible=resolved.flags!=null && resolved.voice!=null;
        boolean writingCompatible=resolved.flags!=null && resolved.writing;
        FeatureConfig.compatibility(voiceCompatible,writingCompatible);
        CloudRuntimeStatus.send(hostContext,"COMPAT","rambler="+voiceCompatible
                +" writing="+writingCompatible+" resolver="+resolved.source);
        CloudRuntimeStatus.send(hostContext,"INIT","rambler="+FeatureConfig.rambler()
                +" writingTools="+FeatureConfig.writingTools()+" version="+gboardVersion(context));
        Log.i(TAG,"Compatibility rambler="+voiceCompatible+" writing="+writingCompatible
                +" resolver="+resolved.source);
        reportRamblerSelection();
        boolean voiceInstalled=voiceCompatible && installRambler(loader,resolved.voice);
        boolean getterInstalled=false;
        if (resolved.flags!=null)
            getterInstalled=installFlagGetter(resolved.flags);
        CloudRuntimeStatus.send(hostContext,"HOOKS",
                "ramblerRequested="+FeatureConfig.ramblerRequested()
                +" writingRequested="+FeatureConfig.writingRequested()
                +" ramblerInstalled="+(FeatureConfig.rambler() && voiceInstalled && getterInstalled)
                +" writingInstalled="+(FeatureConfig.writingTools() && getterInstalled)
                +" getterInstalled="+getterInstalled
                +" extrasRequested="+FeatureConfig.extrasRequested()
                +" extrasInstalled="+(FeatureConfig.extrasRequested() && getterInstalled));
        boolean roundedPreference = installNativePreferences(loader);
        RuntimeFeatureHooks.install(this, hostContext, loader);
        RamblerUiHooks.install(this, hostContext, loader);
        CloudRuntimeStatus.send(hostContext,"EXTRAS",
                "scanText="+FeatureConfig.scanText()+" screenshot="+FeatureConfig.screenshot()
                +" roundedKeys="+FeatureConfig.roundedKeys()+" getter="+getterInstalled
                +" roundedPreference="+roundedPreference+" nativePreferences="+roundedPreference+"; "+HostFeaturePermissions.summary(hostContext));
        CloudRuntimeStatus.send(hostContext,"UPSTREAM",
                "requested="+FeatureConfig.upstreamSummary()+" getter="+getterInstalled);
        FeatureConfig.compatibility(
                voiceCompatible && voiceInstalled && getterInstalled,
                writingCompatible && getterInstalled);
        if (FeatureConfig.writingTools()) {
            WritingToolsDiagnostics.probe(loader);
            if (FeatureConfig.diagnostics() && verifiedWritingTools184) observeWritingTools184State(loader);
            Log.i(TAG,"Writing Tools basic cloud; V2=false; backend=GBOARD_SERVER; local=false");
        }
    }
    private boolean installNativePreferences(ClassLoader loader) {
        try {
            NativeKeyboardPreferences preferences = new NativeKeyboardPreferences(hostContext);
            Method getter = Class.forName("android.app.SharedPreferencesImpl", false, loader)
                    .getDeclaredMethod("getBoolean", String.class, boolean.class);
            hook(getter).intercept(chain -> {
                Object original = chain.proceed();
                String key = (String) chain.getArgs().get(0);
                if (!(original instanceof Boolean)) return original;
                boolean result = preferences.override(chain.getThisObject(), key, (Boolean) original);
                if (FeatureConfig.diagnostics() && !original.equals(result)
                        && writingFlagsLogged.add("native-pref:" + key))
                    CloudRuntimeStatus.send(hostContext, "NATIVE_PREF", key + " changed=true");
                return result;
            });
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            Log.w(TAG,"Native preference hook could not be installed",error);
            return false;
        }
    }
    private String gboardVersion(Context context) {
        try { return context.getPackageManager().getPackageInfo(GBOARD,0).versionName; }
        catch (Exception e) { return "unknown"; }
    }

    private boolean installRambler(ClassLoader loader, RamblerBindings.Profile profile) {
        Log.i(TAG, "Resolved selector signature family: " + profile.id);
        boolean installed=true;
        installed &= hookScope(profile.layout, loader, false);
        installed &= hookScope(profile.setup, loader, false);
        installed &= hookScope(profile.create, loader, false);
        installed &= hookScope(profile.defaults, loader, true);
        installed &= observeSelectionRead(profile.read, loader);
        installed &= observeSelectionWrite(profile.write, loader);
        return installed;
    }

    private boolean hookScope(TargetMethod target, ClassLoader loader, boolean defaults) {
        try {
            hook(target.resolve(loader)).intercept(chain -> {
                if (defaults) RamblerScope.enterDefault(); else RamblerScope.enterVoice();
                try {
                    reportRamblerSelection();
                    return chain.proceed();
                }
                finally {
                    if (defaults) RamblerScope.exitDefault(); else RamblerScope.exitVoice();
                }
            });
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            Log.w(TAG, "Scope hook skipped: " + target, e);
            return false;
        }
    }

    private boolean observeSelectionRead(TargetMethod target, ClassLoader loader) {
        try {
            hook(target.resolve(loader)).intercept(chain -> {
                Object result = chain.proceed();
                if (result instanceof Boolean) {
                    RamblerSelection.observe((Boolean) result, hostContext);
                    reportRamblerSelection();
                }
                return result;
            });
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            Log.w(TAG, "Selection read observer skipped: " + target, e);
            return false;
        }
    }

    private boolean observeSelectionWrite(TargetMethod target, ClassLoader loader) {
        try {
            hook(target.resolve(loader)).intercept(chain -> {
                Object result = chain.proceed();
                reportRamblerSelection();
                if (FeatureConfig.diagnostics()) Log.d(TAG, "Official selector write completed (no forced selection)");
                return result;
            });
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            Log.w(TAG, "Selection write observer skipped: " + target, e);
            return false;
        }
    }

    private boolean installFlagGetter(RamblerFlagBindings.Binding binding) {
        try {
            hook(binding.getter).intercept(chain -> {
                Object original = chain.proceed();
                Object receiver = chain.getThisObject();
                if (receiver == null) return original;
                try {
                    String flag = binding.name(receiver);
                    if (!RamblerFlagPolicy.targeted(flag) && !WritingToolsFlagPolicy.targeted(flag) && !KeyboardFeaturePolicy.targeted(flag)) return original;
                    Context context = hostContext;
                    if (context == null || !GBOARD.equals(context.getPackageName()))
                        return original;
                    // Use the Gboard-owned attach context, avoiding early ActivityThread nulls.
                    Object writingResult = WritingToolsFlagPolicy.transform(flag, original, context);
                    if (FeatureConfig.writingTools() && WritingToolsFlagPolicy.isToolbarFlag(flag)) {
                        int reads = writingToolbarReadCount.incrementAndGet();
                        if (reads <= 5)
                            Log.i(TAG, "WritingTools toolbar gate READ #" + reads
                                    + " changed=" + !java.util.Objects.equals(original, writingResult));
                    }
                    if (FeatureConfig.writingTools() && WritingToolsFlagPolicy.targeted(flag)
                            && writingFlagsLogged.add(flag)) {
                        CloudRuntimeStatus.send(hostContext, "FLAG", flag + " changed="
                                + !java.util.Objects.equals(original, writingResult));
                        Log.i(TAG, "WritingTools flag observed: " + flag
                                + " type=" + (original == null ? "null" : original.getClass().getSimpleName())
                                + " changed=" + !java.util.Objects.equals(original, writingResult));
                    }
                    Object ramblerResult = RamblerFlagPolicy.transform(flag, writingResult, context);
                    if (FeatureConfig.diagnostics() && FeatureConfig.ramblerRequested()
                            && RamblerFlagPolicy.targeted(flag)) {
                        String detail = flag + " type=" + (original == null ? "null" : original.getClass().getSimpleName())
                                + " changed=" + !java.util.Objects.equals(writingResult, ramblerResult)
                                + " " + RamblerSelection.diagnostic(context);
                        if (ramblerDiagnostics.add("flag:" + detail))
                            CloudRuntimeStatus.send(context, "RAMBLER_FLAG", detail);
                    }
                    Object result = KeyboardFeaturePolicy.transform(flag, ramblerResult);
                    if (KeyboardFeaturePolicy.targeted(flag) && FeatureConfig.diagnostics()
                            && writingFlagsLogged.add("extra:"+flag))
                        CloudRuntimeStatus.send(context,"EXTRA_FLAG",flag+" changed="+!java.util.Objects.equals(original,result));
                    return result;
                } catch (ReflectiveOperationException | RuntimeException e) {
                    Log.w(TAG, "Flag transform failed; stock value returned", e);
                    return original;
                }
            });
            CloudRuntimeStatus.send(hostContext, "GETTER", "installed profile=" + binding.profile);
            Log.i(TAG, "Installed shared guarded Rambler/WritingTools flag policy: " + binding.owner
                    + " / profile=" + binding.profile);
            return true;
        } catch (RuntimeException | LinkageError e) {
            Log.w(TAG, "Flag getter hook skipped", e);
            return false;
        }
    }

    private void reportRamblerSelection() {
        if (!FeatureConfig.diagnostics()) return;
        try {
            String detail = RamblerSelection.diagnostic(hostContext);
            if (ramblerDiagnostics.add("selection:" + detail))
                CloudRuntimeStatus.send(hostContext, "RAMBLER_SELECTION", detail);
        } catch (RuntimeException error) {
            Log.w(TAG, "Rambler selection diagnostic unavailable", error);
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
            Method backend = new TargetMethod("qyn", "a", "qym").resolve(loader);
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
                            // The menu already reads this backend. Avoid initializing it in onCreate.
                            if (target == menu) {
                                Object value = backend.invoke(null);
                                line.append(" backend=").append(value instanceof Enum<?>
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

}


