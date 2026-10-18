package dev.kaede.pixelboard;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import java.lang.reflect.Method;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;

/** Gboard-only modern LSPosed entry; fail-closed reflection and disabled feature switches. */
public final class PixelBoardModule extends XposedModule {
    private static final String TAG = "PixelBoardLS";
    private static final String GBOARD = "com.google.android.inputmethod.latin";

    @Override public void onPackageReady(PackageReadyParam param) {
        if (!GBOARD.equals(param.getPackageName())) return;
        ClassLoader loader = param.getClassLoader();
        if (loader == null) { Log.w(TAG, "No Gboard classloader"); return; }
        Log.i(TAG, "Attached to Gboard on SDK " + Build.VERSION.SDK_INT);
        if (FeatureConfig.ENABLE_INLINE_SPELLCHECKER) installSpellChecker(loader);
        if (FeatureConfig.ENABLE_RAMBLER || FeatureConfig.ENABLE_DIAGNOSTIC_HOOKS)
            installRambler(loader);
        if (FeatureConfig.ENABLE_WRITING_TOOLS)
            Log.w(TAG, "Writing Tools not implemented; skipping");
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
                if (FeatureConfig.ENABLE_DIAGNOSTIC_HOOKS)
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
                    if (!RamblerFlagPolicy.targeted(flag)) return original;
                    Context context = GboardContext.current();
                    if (context == null) return original;
                    // Only if explicitly enabled AND a known profile and host context match.
                    return RamblerFlagPolicy.transform(flag, original, context);
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
