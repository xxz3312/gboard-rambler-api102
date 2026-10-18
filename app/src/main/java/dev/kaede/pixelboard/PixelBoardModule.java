package dev.kaede.pixelboard;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import java.lang.reflect.Method;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;

/** Gboard-only modern API 102 entry. Never mutates the installed APK. */
public final class PixelBoardModule extends XposedModule {
    private static final String TAG = "PixelBoardLS";
    private static final String GBOARD = "com.google.android.inputmethod.latin";

    @Override public void onPackageReady(PackageReadyParam param) {
        if (!GBOARD.equals(param.getPackageName())) return;
        ClassLoader loader = param.getClassLoader();
        if (loader == null) { Log.w(TAG, "No Gboard classloader"); return; }
        Log.i(TAG, "Attached to Gboard on SDK " + Build.VERSION.SDK_INT);
        if (FeatureConfig.ENABLE_INLINE_SPELLCHECKER) installSpellChecker(loader);
        if (FeatureConfig.ENABLE_RAMBLER || FeatureConfig.ENABLE_DIAGNOSTIC_HOOKS) {
            installRambler(loader);
        }
        if (FeatureConfig.ENABLE_WRITING_TOOLS)
            Log.w(TAG, "Writing Tools not implemented; skipping");
    }

    private void installRambler(ClassLoader loader) {
        RamblerBindings.Profile profile = RamblerBindings.select(loader);
        if (profile == null) { Log.w(TAG, "No unambiguous Rambler profile; skipped"); return; }
        Log.i(TAG, "Resolved selector profile " + profile.id);
        hookScope(profile.layout, loader, false);
        hookScope(profile.setup, loader, false);
        hookScope(profile.create, loader, false);
        hookScope(profile.defaults, loader, true);
        observeSelectionRead(profile.read, loader);
        observeSelectionWrite(profile.write, loader);
        if (FeatureConfig.ENABLE_RAMBLER) installFlagGetter(loader, profile.id);
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
            Log.w(TAG, "Lifecycle hook skipped: " + target, e);
        }
    }

    private void observeSelectionRead(TargetMethod target, ClassLoader loader) {
        try {
            hook(target.resolve(loader)).intercept(chain -> {
                Object result = chain.proceed();
                if (result instanceof Boolean) {
                    Context context = chain.getArgs().isEmpty() ? null
                            : chain.getArgs().get(0) instanceof Context
                            ? (Context)chain.getArgs().get(0) : null;
                    RamblerSelection.observe((Boolean)result, context);
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
                // Writer is a void member; its first argument is not necessarily the
                // selected mode. Never guess from that boolean alone.
                if (FeatureConfig.ENABLE_DIAGNOSTIC_HOOKS)
                    Log.d(TAG, "Official selector write completed");
                return result;
            });
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            Log.w(TAG, "Selection write observer skipped: " + target, e);
        }
    }

    private void installFlagGetter(ClassLoader loader, String profileId) {
        RamblerFlagBindings.Binding binding = RamblerFlagBindings.resolve(loader, profileId);
        if (binding == null) {
            Log.w(TAG, "No unique flag getter binding; feature flags untouched"); return;
        }
        try {
            hook(binding.getter).intercept(chain -> {
                Object original = chain.proceed();
                Object receiver = chain.getThisObject();
                if (receiver == null) return original;
                try {
                    String flag = binding.name(receiver);
                    if (!RamblerFlagPolicy.targeted(flag)) return original;
                    // Context acquisition is not yet validated. With a null context,
                    // the policy remains conservative regarding explicit prefs only
                    // when the app's selection observer supplied a verified state.
                    // Do not modify values until a verified app Context is available.
                    Context context = null;
                    if (context == null) return original;
                    return RamblerFlagPolicy.transform(flag, original, context);
                } catch (ReflectiveOperationException | RuntimeException e) {
                    Log.w(TAG, "Flag evaluation failed; preserving original", e);
                    return original;
                }
            });
            Log.i(TAG, "Flag getter intercepted in passthrough mode: " + binding.owner);
        } catch (RuntimeException | LinkageError ex) {
            Log.w(TAG, "Flag getter hook failed; stock values retained", ex);
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
