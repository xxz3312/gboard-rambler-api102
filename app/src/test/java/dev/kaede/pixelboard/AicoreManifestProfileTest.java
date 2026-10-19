package dev.kaede.pixelboard;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class AicoreManifestProfileTest {
    @Test public void inspectedProfilesRequireExactPackageVersion() {
        AicoreManifestProfile p = AicoreManifestProfile.forPackage(513248L,
                "0.thirdpartyexperimental.qc.ffdf_aicore_20260820.00_RC09.974378940");
        assertNotNull(p);
        assertEquals("iqw", p.clientClass);
        assertEquals("irx", p.fetchClass);
        assertEquals("nkf", p.continuationClass);
        assertNull(AicoreManifestProfile.forPackage(513248L, "other"));
        assertNull(AicoreManifestProfile.forPackage(513249L,
                "0.thirdpartyexperimental.qc.ffdf_aicore_20260820.00_RC09.974378940"));
    }
    @Test public void reportWhitelistsMetadataAndPreservesOriginalRequest() {
        Map<String, String> labels = new HashMap<>();
        labels.put("build_id", "20063");
        labels.put("device_tier", "");
        labels.put("variant", "VARIANT_UNSPECIFIED");
        labels.put("client_group", "all\nother");
        labels.put("prompt", "PRIVATE_DRAFT");
        Map<String, String> original = new HashMap<>(labels);
        String result = AicoreManifestProfile.describe(Collections.unmodifiableMap(labels));
        assertTrue(result.contains("build_id=20063"));
        assertTrue(result.contains("device_tier=<empty>"));
        assertFalse(result.contains("PRIVATE_DRAFT"));
        assertFalse(result.contains("\n"));
        assertEquals(original, labels);
    }
    @Test public void missingFieldsAreExplicitAndValuesAreBounded() {
        Map<String, String> labels = Collections.singletonMap("client_group", String.join("", Collections.nCopies(300, "x")));
        String result = AicoreManifestProfile.describe(labels);
        assertTrue(result.contains("build_id=<absent>"));
        assertTrue(result.endsWith(String.join("", Collections.nCopies(96, "x"))));
    }
}
