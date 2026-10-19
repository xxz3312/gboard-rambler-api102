package dev.kaede.pixelboard;

import java.util.Map;

/** Exact APK profiles inspected from DEX; request observation never edits the map. */
final class AicoreManifestProfile {
    final String clientClass, fetchClass, continuationClass;
    private AicoreManifestProfile(String client, String fetch, String continuation) {
        clientClass = client; fetchClass = fetch; continuationClass = continuation;
    }
    static AicoreManifestProfile forPackage(long code, String version) {
        if (code == 513248L && "0.thirdpartyexperimental.qc.ffdf_aicore_20260820.00_RC09.974378940".equals(version))
            return new AicoreManifestProfile("iqw", "irx", "nkf");
        if (code == 526928L && "0.release.qc8635.prod_aicore_20260917.00_RC01".equals(version))
            return new AicoreManifestProfile("isz", "ity", "nrj");
        return null;
    }
    static String describe(Map<?, ?> labels) {
        StringBuilder line = new StringBuilder("AICore manifest labels (unchanged):");
        for (String key : new String[] {"build_id", "device_tier", "variant", "client_group"}) {
            Object value = labels.get(key);
            String text = value instanceof String ? (String) value : "<absent>";
            line.append(" ").append(key).append("=").append(text.isEmpty() ? "<empty>"
                    : text.substring(0, Math.min(96, text.length()))
                        .replace('\n', ' ').replace('\r', ' '));
        }
        return line.toString();
    }
}
