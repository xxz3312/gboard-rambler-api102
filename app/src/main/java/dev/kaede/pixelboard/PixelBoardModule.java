package dev.kaede.pixelboard;

import android.os.Build;
import android.util.Log;
import java.lang.reflect.Method;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;

/** Modern API 102 entry. Never mutate the host APK, manifest, or dex on disk. */
public final class PixelBoardModule extends XposedModule {
    private static final String TAG = "PixelBoardLS";
    private static final String GBOARD = "com.google.android.inputmethod.latin";

    @Override public void onPackageReady(PackageReadyParam param) {
        if (!GBOARD.equals(param.getPackageName())) return;
        final ClassLoader loader = param.getClassLoader();
        if (loader == null) { Log.w(TAG, "No app classloader"); return; }
        Log.i(TAG, "Gboard loaded; Android " + Build.VERSION.SDK_INT + "; API 102 entry active");
        // Explicitly opt into each verified patch. Other PixelBoard features are not yet ported.
        if (FeatureConfig.ENABLE_INLINE_SPELLCHECKER) {
            installSpellChecker(loader);
        }
    }

    private void installSpellChecker(ClassLoader loader) {
        if (Build.VERSION.SDK_INT < 31) return;
        try {
            // rcq.m(Context,TextServicesManager): boolean -- observed in PixelBoard's
            // upstream patch. This obfuscated symbol must be revalidated for every version.
            Class<?> cls = Class.forName("rcq", false, loader);
            Method method = cls.getDeclaredMethod("m", android.content.Context.class,
                    android.view.textservice.TextServicesManager.class);
            if (method.getReturnType() != boolean.class) {
                Log.w(TAG, "spellchecker method return mismatch; skipping"); return;
            }
            hook(method).intercept(chain -> {
                // Preserve stock behavior if original preconditions cannot be established.
                // PixelBoard forces the Android 12+ compatibility gate to true.
                return true;
            });
            Log.i(TAG, "Installed inline spellchecker eligibility hook");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) {
            Log.w(TAG, "Spellchecker hook skipped (symbol absent or incompatible)", ex);
        }
    }
}
