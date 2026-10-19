package dev.kaede.pixelboard;

import android.os.Build;
import java.lang.reflect.Field;

/** Only invoked from the verified Gboard Application context. Never writes system properties. */
public final class GboardPixelIdentity {
    private static final PixelIdentityOverride.Fields FIELDS = new PixelIdentityOverride.Fields() {
        private Field resolve(String name) throws ReflectiveOperationException {
            Field field = Build.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        }
        @Override public String get(String name) throws Exception { return (String) resolve(name).get(null); }
        @Override public void set(String name, String value) throws Exception { resolve(name).set(null, value); }
    };
    private static volatile PixelIdentityOverride.Result result;
    private GboardPixelIdentity() {}
    public static void apply(String packageName, boolean enabled) {
        result = PixelIdentityOverride.apply(packageName, enabled, FIELDS);
    }
    public static String summary() {
        PixelIdentityOverride.Result current = result;
        if (current == null) return "pixelIdentity=NOT_INITIALIZED";
        String state = current.outcome.name();
        if (current.outcome == PixelIdentityOverride.Outcome.APPLIED) {
            try { if (!PixelIdentityOverride.matches(FIELDS)) state = "DRIFTED"; }
            catch (Exception | LinkageError e) { state = "READBACK_FAILED"; }
        }
        return "pixelIdentity=" + state;
    }
    public static String details() {
        PixelIdentityOverride.Result current = result;
        return summary() + " phase=context_ready profile=Pixel9Pro profileFields=5"
                + (current == null || current.error.isEmpty() ? "" : " error=" + current.error)
                + " model=" + Build.MODEL + " device=" + Build.DEVICE
                + " attestation=unchanged";
    }
}
