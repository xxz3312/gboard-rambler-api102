package dev.kaede.pixelboard;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class PixelIdentityOverrideTest {
    private static final String GBOARD = "com.google.android.inputmethod.latin";
    private static class FakeFields implements PixelIdentityOverride.Fields {
        final Map<String, String> values = new HashMap<>();
        int reads, writes;
        FakeFields() {
            for (String field : new String[] {"MANUFACTURER", "BRAND", "MODEL", "DEVICE", "PRODUCT",
                    "FINGERPRINT", "HARDWARE", "SOC_MODEL"}) values.put(field, "original-" + field);
        }
        public String get(String name) throws Exception { reads++; return values.get(name); }
        public void set(String name, String value) throws Exception { writes++; values.put(name, value); }
    }
    @Test public void disabledAndBackendPackagesCannotReadOrWriteBuildFields() {
        FakeFields fields = new FakeFields();
        assertEquals(PixelIdentityOverride.Outcome.DISABLED,
                PixelIdentityOverride.apply(GBOARD, false, fields).outcome);
        for (String pkg : new String[] {"com.google.android.aicore", "com.google.android.as.oss",
                "dev.kaede.pixelboard", "com.google.android.gms", null}) {
            assertEquals(PixelIdentityOverride.Outcome.WRONG_PACKAGE,
                    PixelIdentityOverride.apply(pkg, true, fields).outcome);
        }
        assertEquals(0, fields.reads);
        assertEquals(0, fields.writes);
    }
    @Test public void onlyIdentityFieldsChangeAndReadbackDetectsDrift() throws Exception {
        FakeFields fields = new FakeFields();
        assertEquals(PixelIdentityOverride.Outcome.APPLIED,
                PixelIdentityOverride.apply(GBOARD, true, fields).outcome);
        assertEquals("Pixel 9 Pro", fields.values.get("MODEL"));
        assertEquals("caiman", fields.values.get("DEVICE"));
        assertEquals(5, fields.writes);
        for (String name : new String[] {"FINGERPRINT", "HARDWARE", "SOC_MODEL"})
            assertEquals("original-" + name, fields.values.get(name));
        assertTrue(PixelIdentityOverride.matches(fields));
        fields.values.put("MODEL", "other module");
        assertFalse(PixelIdentityOverride.matches(fields));
    }
    @Test public void preflightReadFailureNeverStartsMutation() {
        FakeFields fields = new FakeFields() {
            @Override public String get(String name) throws Exception { throw new IllegalAccessException(); }
        };
        assertEquals(PixelIdentityOverride.Outcome.FAILED_UNCHANGED,
                PixelIdentityOverride.apply(GBOARD, true, fields).outcome);
        assertEquals(0, fields.writes);
    }
    @Test public void failureAfterMutationRestoresOriginalValues() {
        FakeFields fields = new FakeFields() {
            boolean thrown;
            @Override public void set(String name, String value) throws Exception {
                super.set(name, value);
                if ("MODEL".equals(name) && !thrown) { thrown = true; throw new IllegalAccessException(); }
            }
        };
        Map<String, String> before = new HashMap<>(fields.values);
        assertEquals(PixelIdentityOverride.Outcome.FAILED_RESTORED,
                PixelIdentityOverride.apply(GBOARD, true, fields).outcome);
        assertEquals(before, fields.values);
    }
    @Test public void silentlyIgnoredWriteIsNotReportedAsApplied() {
        FakeFields fields = new FakeFields() {
            @Override public void set(String name, String value) throws Exception {
                if (!"DEVICE".equals(name)) super.set(name, value);
            }
        };
        Map<String, String> before = new HashMap<>(fields.values);
        assertEquals(PixelIdentityOverride.Outcome.FAILED_RESTORED,
                PixelIdentityOverride.apply(GBOARD, true, fields).outcome);
        assertEquals(before, fields.values);
    }
    @Test public void failedRollbackIsReportedRatherThanClaimingOriginalState() {
        FakeFields fields = new FakeFields() {
            @Override public void set(String name, String value) throws Exception {
                if ("MODEL".equals(name) && "Pixel 9 Pro".equals(value)) throw new IllegalAccessException();
                if ("MANUFACTURER".equals(name) && value.startsWith("original-")) throw new IllegalAccessException();
                super.set(name, value);
            }
        };
        assertEquals(PixelIdentityOverride.Outcome.FAILED_PARTIAL,
                PixelIdentityOverride.apply(GBOARD, true, fields).outcome);
        assertEquals("Google", fields.values.get("MANUFACTURER"));
    }
    @Test public void writeDeniedBeforeMutationDoesNotAttemptRestoreOrClaimPartialChange() {
        FakeFields fields = new FakeFields() {
            @Override public void set(String name, String value) throws Exception {
                writes++;
                throw new IllegalAccessException();
            }
        };
        Map<String, String> original = new HashMap<>(fields.values);
        assertEquals(PixelIdentityOverride.Outcome.FAILED_UNCHANGED,
                PixelIdentityOverride.apply(GBOARD, true, fields).outcome);
        assertEquals(1, fields.writes);
        assertEquals(original, fields.values);
    }
    @Test public void restoreThatThrowsAfterRestoringIsJudgedByReadback() {
        FakeFields fields = new FakeFields() {
            @Override public void set(String name, String value) throws Exception {
                super.set(name, value);
                if ("MODEL".equals(name)) throw new IllegalAccessException();
            }
        };
        Map<String, String> original = new HashMap<>(fields.values);
        assertEquals(PixelIdentityOverride.Outcome.FAILED_RESTORED,
                PixelIdentityOverride.apply(GBOARD, true, fields).outcome);
        assertEquals(original, fields.values);
    }
}

