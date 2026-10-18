package dev.kaede.pixelboard;

import android.content.Context;
import android.app.Application;
import java.util.concurrent.atomic.AtomicBoolean;
import android.os.Build;
import android.util.Log;
import java.lang.reflect.Method;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;

/** Gboard-only modern LSPosed entry; fail-closed reflection and disabled feature switches. */
public final class PixelBoardModule extends XposedModule {
    private static final String TAG = "PixelBoardLS";
    private static final String GBOARD = "com.google.android.inputmethod.latin";

    private final AtomicBoolean bootstrapped = new AtomicBoolean(false);

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
        FeatureConfig.initialize(this);
        Log.i(TAG, "Gboard Application Context available; SDK " + Build.VERSION.SDK_INT
              + "; Rambler=" + FeatureConfig.rambler()
              + "; diagnostics=" + FeatureConfig.diagnostics());
        if (FeatureConfig.spellchecker()) installSpellChecker(loader);
        if (FeatureConfig.rambler() || FeatureConfig.diagnostics() || FeatureConfig.writingTools()) installRambler(loader);
        if (FeatureConfig.writingTools())
            Log.i(TAG, "Writing Tools flag policy enabled (runtime only; no DEX patch)");
    }

    private void installRambler(ClassLoader loader) {
        RamblerBindings.Profile profile = RamblerBindings.select(loader);
        if (profile == null) { Log.w(TAG, "No unique verified selector signatures; skipped"); return; }
        Log.i(TAG, "Resolved selector signature family: " + profile.id);
        hookScope(profile.layout, loader, false);
        hookScope(profile.setup, loader, false);
        hookScope(profile.create, loader, false);
        hookScope(profile.defaults, loader, true);
        observeSelectionRead(profile.read, loader);
        observeSelectionWrite(profile.write, loader);
        if (FeatureConfig.rambler() || FeatureConfig.writingTools()) installFlagGetter(loader, profile.id);
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

    private void installFlagGetter(ClassLoader loader, String profile) {
        RamblerFlagBindings.Binding binding = RamblerFlagBindings.resolve(loader, profile);
        if (binding == null) {
            Log.w(TAG, "No unique flag getter binding; leaving values intact"); return;
        }
        try {
            hook(binding.getter).intercept(chain -> {
                Object original = chain.proceed();
                Object receiver = chain.getThisObject();
                if (receiver == null) return original;
                try {
                    String flag = binding.name(receiver);
                    if (!RamblerFlagPolicy.targeted(flag) && !WritingToolsFlagPolicy.targeted(flag)) return original;
                    Context context = GboardContext.current();
                    if (context == null) return original;
                    // Only if explicitly enabled AND a known profile and host context match.
                    Object writingResult = WritingToolsFlagPolicy.transform(flag, original, context);
                    return RamblerFlagPolicy.transform(flag, writingResult, context);
                } catch (ReflectiveOperationException | RuntimeException e) {
                    Log.w(TAG, "Flag transform failed; stock value returned", e);
                    return original;
                }
            });
            Log.i(TAG, "Installed guarded Rambler flag policy: " + binding.owner);
        } catch (RuntimeException | LinkageError e) {
            Log.w(TAG, "Flag getter hook skipped", e);
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
