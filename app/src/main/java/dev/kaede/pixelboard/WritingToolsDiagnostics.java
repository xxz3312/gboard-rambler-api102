package dev.kaede.pixelboard;

import android.util.Log;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
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
        probeSignals(loader);
        probeGetter(loader);
        Log.i(TAG, "WritingTools 18.0.3 probe: " + matched + "/" + targets.length
                + " signatures found; no initialization/signal hooks installed");
    }
    private static void probeGetter(ClassLoader loader) {
        RamblerFlagBindings.Binding binding = RamblerFlagBindings.resolveUnique(loader);
        if (binding == null) {
            Log.w(TAG, "WritingTools getter: no unique typed binding; runtime overrides unavailable");
        } else {
            Log.i(TAG, "WritingTools getter: unique typed binding " + binding.profile
                    + " / " + binding.owner + " (method not invoked by probe)");
        }
    }

    private static void probeSignals(ClassLoader loader) {
        // PixelBoard runtime targets. Resolve identity without reading or changing results.
        String[][] specs = {{"gvu", "a"}, {"lvi", "b"}, {"gqd", "a"}};
        for (String[] spec : specs) {
            try {
                Class<?> owner = Class.forName(spec[0], false, loader);
                Field field = owner.getDeclaredField(spec[1]);
                if (!Modifier.isStatic(field.getModifiers())) {
                    Log.i(TAG, "WritingTools signal rejected non-static: " + spec[0] + "#" + spec[1]);
                    continue;
                }
                Log.i(TAG, "WritingTools signal descriptor found: " + spec[0] + "#" + spec[1]
                        + " type=" + field.getType().getName());
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                Log.i(TAG, "WritingTools signal unavailable: " + spec[0] + "#" + spec[1]);
            }
        }
    }
}
