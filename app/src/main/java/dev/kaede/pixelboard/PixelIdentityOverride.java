package dev.kaede.pixelboard;

import java.util.LinkedHashMap;
import java.util.Map;

/** Process-local Build identity experiment, deliberately independent of model/auth policy. */
public final class PixelIdentityOverride {
    public interface Fields {
        String get(String name) throws Exception;
        void set(String name, String value) throws Exception;
    }
    public enum Outcome { DISABLED, WRONG_PACKAGE, APPLIED, FAILED_UNCHANGED, FAILED_RESTORED, FAILED_PARTIAL }
    public static final class Result {
        public final Outcome outcome;
        public final String error;
        Result(Outcome outcome, String error) { this.outcome = outcome; this.error = error; }
    }
    private PixelIdentityOverride() {}

    private static Map<String, String> profile() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("MANUFACTURER", "Google");
        values.put("BRAND", "google");
        values.put("MODEL", "Pixel 9 Pro");
        values.put("DEVICE", "caiman");
        values.put("PRODUCT", "caiman");
        return values;
    }
    public static boolean matches(Fields fields) throws Exception {
        for (Map.Entry<String, String> entry : profile().entrySet())
            if (!entry.getValue().equals(fields.get(entry.getKey()))) return false;
        return true;
    }
    public static Result apply(String packageName, boolean enabled, Fields fields) {
        if (!"com.google.android.inputmethod.latin".equals(packageName))
            return new Result(Outcome.WRONG_PACKAGE, "");
        if (!enabled) return new Result(Outcome.DISABLED, "");
        Map<String, String> original = new LinkedHashMap<>();
        try {
            for (String key : profile().keySet()) original.put(key, fields.get(key));
        } catch (Exception | LinkageError e) {
            return new Result(Outcome.FAILED_UNCHANGED, e.getClass().getSimpleName());
        }
        java.util.Set<String> attempted = new java.util.LinkedHashSet<>();
        try {
            for (Map.Entry<String, String> entry : profile().entrySet()) {
                if (java.util.Objects.equals(original.get(entry.getKey()), entry.getValue())) continue;
                attempted.add(entry.getKey());
                fields.set(entry.getKey(), entry.getValue());
            }
            if (!matches(fields)) throw new IllegalStateException("readback");
            return new Result(Outcome.APPLIED, "");
        } catch (Exception | LinkageError e) {
            boolean observedChange = false;
            for (String name : attempted) {
                boolean needsRestore;
                try { needsRestore = !java.util.Objects.equals(original.get(name), fields.get(name)); }
                catch (Exception | LinkageError ignored) { needsRestore = true; }
                if (!needsRestore) continue;
                observedChange = true;
                try { fields.set(name, original.get(name)); }
                catch (Exception | LinkageError ignored) { /* Judge restoration by final readback. */ }
            }
            boolean restored = true;
            for (Map.Entry<String, String> entry : original.entrySet()) {
                try { if (!java.util.Objects.equals(entry.getValue(), fields.get(entry.getKey()))) restored = false; }
                catch (Exception | LinkageError ignored) { restored = false; }
            }
            return new Result(restored ? (observedChange ? Outcome.FAILED_RESTORED : Outcome.FAILED_UNCHANGED)
                    : Outcome.FAILED_PARTIAL,
                    e.getClass().getSimpleName());
        }
    }
}

