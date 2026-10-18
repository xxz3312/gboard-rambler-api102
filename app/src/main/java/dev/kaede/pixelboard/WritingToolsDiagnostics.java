package dev.kaede.pixelboard;

import android.util.Log;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Strict read-only probes for PixelBoard's Gboard 18.0.3 DEX targets.
 * This NEVER invokes the methods or alters signals, preferences, or consent.
 */
public final class WritingToolsDiagnostics {
    private static final String TAG = "PixelBoardLS";
    private WritingToolsDiagnostics() {}

    public static void probe(ClassLoader loader) {
        int matched = 0;
        TargetMethod[] targets = WritingToolsBindings.candidates1803();
        for (TargetMethod target : targets) {
            try {
                Method method = target.resolve(loader);
                if (Modifier.isAbstract(method.getModifiers())) {
                    Log.w(TAG, "WritingTools probe rejected abstract target: " + target);
                    continue;
                }
                matched++;
                Log.i(TAG, "WritingTools target found (read-only): " + target);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                Log.i(TAG, "WritingTools target absent/incompatible: " + target);
            }
        }
        Log.i(TAG, "WritingTools 18.0.3 probe: " + matched + "/" + targets.length
                + " signatures found; no initialization/signal hooks installed");
    }
}
