package dev.kaede.pixelboard;

import android.os.Build;
import android.util.Log;
import java.lang.reflect.Method;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;

/**
 * Gboard-only Modern LSPosed API 102 module.
 * Never modifies Gboard APK, dex, manifest, resources or signing certificate.
 */
public final class PixelBoardModule extends XposedModule {
    private static final String TAG = "PixelBoardLS";
    private static final String GBOARD = "com.google.android.inputmethod.latin";

    @Override public void onPackageReady(PackageReadyParam param) {
        if (!GBOARD.equals(param.getPackageName())) return;
        final ClassLoader loader = param.getClassLoader();
        if (loader == null) { Log.w(TAG, "No Gboard classloader"); return; }
        Log.i(TAG, "Attached to Gboard on SDK " + Build.VERSION.SDK_INT);
        if (FeatureConfig.ENABLE_INLINE_SPELLCHECKER) installSpellChecker(loader);
        if (FeatureConfig.ENABLE_RAMBLER || FeatureConfig.ENABLE_DIAGNOSTIC_HOOKS) installRambler(loader);
        if (FeatureConfig.ENABLE_WRITING_TOOLS)
            Log.w(TAG, "Writing Tools hook is not yet implemented; skipping");
    }

    private void installRambler(ClassLoader loader) {
        RamblerBindings.Profile profile = RamblerBindings.select(loader);
        if (profile == null) { Log.w(TAG, "No unique complete Rambler profile; no hooks installed"); return; }
        Log.i(TAG, "Rambler mapping resolved: " + profile.id);
        // Instrument only selector lifecycle, without overriding Gboard feature/server gates.
        // The scope is used by future, separately tested feature-gate hooks.
        hookScope(profile.layout, loader, false);
        hookScope(profile.setup, loader, false);
        hookScope(profile.create, loader, false);
        hookScope(profile.defaults, loader, true);
        observe(profile.read, loader, "selection read");
        observe(profile.write, loader, "selection write");
    }

    private void hookScope(TargetMethod method, ClassLoader loader, boolean defaults) {
        try {
            hook(method.resolve(loader)).intercept(chain -> {
                if (defaults) RamblerScope.enterDefault(); else RamblerScope.enterVoice();
                try { return chain.proceed(); }
                finally {
                    if (defaults) RamblerScope.exitDefault(); else RamblerScope.exitVoice();
                }
            });
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            Log.w(TAG, "Scope hook skipped: " + method, e);
        }
    }

    private void observe(TargetMethod method, ClassLoader loader, String label) {
        try {
            hook(method.resolve(loader)).intercept(chain -> {
                Object result = chain.proceed();
                if (FeatureConfig.ENABLE_DIAGNOSTIC_HOOKS)
                    Log.d(TAG, label + " (voiceScope=" + RamblerScope.isVoiceSettings() + ")");
                return result;
            });
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            Log.w(TAG, "Observer skipped: " + method, e);
        }
    }

    private void installSpellChecker(ClassLoader loader) {
        if (Build.VERSION.SDK_INT < 31) return;
        try {
            // Candidate for PixelBoard spellchecker utility; signature MUST be version-verified.
            TargetMethod candidate = new TargetMethod("rcq", "m", "boolean",
                    "android.content.Context", "android.view.textservice.TextServicesManager");
            Method method = candidate.resolve(loader);
            hook(method).intercept(chain -> true);
            Log.i(TAG, "Experimental spellchecker compatibility gate enabled");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            Log.w(TAG, "Spellchecker candidate not present; skipping", e);
        }
    }
}
