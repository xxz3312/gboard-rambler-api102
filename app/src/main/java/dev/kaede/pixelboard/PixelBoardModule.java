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

/** Gboard-only modern LSPosed entry; fail-closed reflection and disabled feature switches. */
public final class PixelBoardModule extends XposedModule {
    private static final String TAG = "PixelBoardLS";
    private static final String GBOARD = "com.google.android.inputmethod.latin";

    private final AtomicBoolean bootstrapped = new AtomicBoolean(false);
    private final java.util.Set<String> writingFlagsLogged = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final AtomicInteger writingToolbarReadCount = new AtomicInteger(0);
    private final AtomicInteger toolbarConsumerLogCount = new AtomicInteger(0);
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

    private void initializeForGboard(ClassLoader loader, Context context) {
        hostContext = context.getApplicationContext() == null ? context : context.getApplicationContext();
        FeatureConfig.initialize(this);
        try {
            PackageInfo pkg = context.getPackageManager().getPackageInfo(GBOARD, 0);
            Log.i(TAG, "Target Gboard version=" + pkg.versionName
                    + ", versionCode=" + pkg.getLongVersionCode());
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Gboard package version unavailable", e);
        }
        Log.i(TAG, "Gboard Application Context available; SDK " + Build.VERSION.SDK_INT
              + "; Rambler=" + FeatureConfig.rambler()
              + "; diagnostics=" + FeatureConfig.diagnostics());
        if (FeatureConfig.spellchecker()) installSpellChecker(loader);
        if (FeatureConfig.writingTools()) {
            WritingToolsDiagnostics.probe(loader);
            observeWritingToolsToolbarConsumer(loader);
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
        if (FeatureConfig.writingTools())
            Log.i(TAG, "Writing Tools policy=basic-cloud-v1; V2=false, backend=GBOARD_SERVER, hybrid=false, onDeviceProofread=false; overrides require resolved getter; UI/backend unverified");
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
                    Object writingResult = WritingToolsFlagPolicy.transform(flag, original, context);
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

    /**
     * Gboard 18.4.1 APK: pmb#n(nsn):String reads plx#n, the V2 toolbar flag.
     * This is an OBSERVER only: never logs user text, arguments or return data.
     */
    private void observeWritingToolsToolbarConsumer(ClassLoader loader) {
        try {
            Method method = new TargetMethod("pmb", "n", "java.lang.String", "nsn")
                    .resolve(loader);
            hook(method).intercept(chain -> {
                Object result = chain.proceed();
                int hit = toolbarConsumerLogCount.incrementAndGet();
                if (hit <= 8)
                    Log.i(TAG, "WritingTools 18.4.1 pmb#n toolbar flag consumer invoked #"
                            + hit + ", returnedNull=" + (result == null));
                return result;
            });
            Log.i(TAG, "WritingTools 18.4.1 pmb#n passive observer installed");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            Log.w(TAG, "WritingTools 18.4.1 pmb#n observer not installed", e);
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
