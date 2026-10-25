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
        probe1841ToolbarReference(loader);
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

    private static void probe1841ToolbarReference(ClassLoader loader) {
        try {
            Class<?> owner = Class.forName("plx", false, loader);
            Field flag = owner.getDeclaredField("n");
            if (!Modifier.isStatic(flag.getModifiers())
                    || !"odr".equals(flag.getType().getName())) {
                Log.w(TAG, "WritingTools 18.4.1 toolbar flag descriptor differs from APK");
                return;
            }
            Method consumer = new TargetMethod("pmb", "n", "java.lang.String", "nsn")
                    .resolve(loader);
            Log.i(TAG, "WritingTools 18.4.1 toolbar flag plx#n (odr), "
                    + "reader pmb#n(nsn):String verified; no hook installed");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            Log.i(TAG, "WritingTools 18.4.1 toolbar reader descriptor not found");
        }
    }

    private static void probeSignals(ClassLoader loader) {
        // PixelBoard runtime targets. Resolve identity without reading or changing results.
        // On 18.4.1, lvi#b is a long field, not a signal object.
        // gqd#a was not verified; do not treat it as a forced signal.
        String[][] specs = {{"gvu", "a"}};
        for (String[] spec : specs) {
            try {
                Class<?> owner = Class.forName(spec[0], false, loader);
                Field field = owner.getDeclaredField(spec[1]);
                if (!Modifier.isStatic(field.getModifiers())) {
                    Log.i(TAG, "WritingTools signal rejected non-static: " + spec[0] + "#" + spec[1]);
                    continue;
                }
                Log.i(TAG, "WritingTools candidate field descriptor (not a verified signal): " + spec[0] + "#" + spec[1]
                        + " type=" + field.getType().getName());
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                Log.i(TAG, "WritingTools signal unavailable: " + spec[0] + "#" + spec[1]);
            }
        }
    }
}
