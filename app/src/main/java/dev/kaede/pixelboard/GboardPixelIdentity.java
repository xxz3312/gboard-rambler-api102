package dev.kaede.pixelboard;

import android.os.Build;
import java.lang.reflect.Field;

/** Only invoked from the verified Gboard Application context. Never writes system properties. */
public final class GboardPixelIdentity {
    private static final String[] ALLOWED = {"MANUFACTURER", "BRAND", "MODEL", "DEVICE", "PRODUCT"};
    private static volatile StaticStringFieldWriter writer;
    private static volatile String mechanism = "reflection";
    private static volatile String beforeModel = "", beforeDevice = "";
    private static final PixelIdentityOverride.Fields FIELDS = new PixelIdentityOverride.Fields() {
        private Field resolve(String name) throws ReflectiveOperationException {
            if (!java.util.Arrays.asList(ALLOWED).contains(name)) throw new NoSuchFieldException("Not allowed");
            Field field = Build.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        }
        @Override public String get(String name) throws Exception { return (String) resolve(name).get(null); }
        @Override public void set(String name, String value) throws Exception {
            Field field = resolve(name);
            if (writer == null) {
                try { field.set(null, value); return; }
                catch (IllegalAccessException denied) {
                    mechanism = "unsafe_unavailable";
                    writer = StaticStringFieldWriter.create(Build.class, ALLOWED);
                    mechanism = writer.mechanism;
                }
            }
            writer.set(name, value);
        }
    };
    private static volatile PixelIdentityOverride.Result result;
    private GboardPixelIdentity() {}
    public static void apply(String packageName, boolean enabled) {
        if (!"com.google.android.inputmethod.latin".equals(packageName)) {
            result = PixelIdentityOverride.apply(packageName, enabled, FIELDS);
            return;
        }
        try { beforeModel = FIELDS.get("MODEL"); beforeDevice = FIELDS.get("DEVICE"); }
        catch (Exception | LinkageError ignored) { }
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
        return summary() + " writer=" + mechanism + " profile=Pixel9Pro"
                + (current == null || current.error.isEmpty() ? "" : " error=" + current.error)
                + " before=" + beforeModel + "/" + beforeDevice
                + " " + currentFields()
                + " attestation=unchanged";
    }
    private static String currentFields() {
        try { return "manufacturer=" + FIELDS.get("MANUFACTURER") + " brand=" + FIELDS.get("BRAND")
                + " model=" + FIELDS.get("MODEL") + " device=" + FIELDS.get("DEVICE")
                + " product=" + FIELDS.get("PRODUCT"); }
        catch (Exception | LinkageError e) { return "fields=READ_FAILED"; }
    }
}

