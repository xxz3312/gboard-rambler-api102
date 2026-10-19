package dev.kaede.pixelboard;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Fixed diagnostic protocol; never accepts intent-supplied package names as identity. */
public final class RuntimeStatusFormat {
    private RuntimeStatusFormat() {}
    private static final Set<String> EVENTS = new HashSet<>(Arrays.asList(
            "GBOARD_IDENTITY", "ASTREA_ATTESTATION", "V2_ASTREA_ERROR", "RUNTIME_ALIVE", "CHANNEL_TEST", "GBOARD_ATTACHED", "INIT_FAILED", "FLAGS_READY", "FLAGS_FAILED",
            "MODE", "STATE", "BACKEND_PACKAGE", "ASTREA_FLAG", "MANIFEST_REQUEST", "MANIFEST_WIRE", "MANIFEST_OBSERVER_READY", "MANIFEST_OBSERVER_UNSUPPORTED",
            "V2_PROFILE_FAILED", "V2_DIAGNOSTICS_READY", "V2_DIAGNOSTICS_FAILED",
            "V2_ROUTE", "V2_SESSION_REQUEST", "V2_SESSION_READY", "V2_SESSION_FAILED",
            "V2_RESPONSE", "V2_PANEL_ERROR", "HANDLER_STATUS", "V2_AVAILABILITY_REQUEST",
            "V2_AVAILABILITY", "V2_AVAILABILITY_FAILED"));
    public static boolean allowedEvent(String code) { return EVENTS.contains(code); }
    public static String metadata(String text) {
        if (text == null) return "";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length() && out.length() < 320; i++) {
            char c = text.charAt(i);
            out.append(c >= 32 && c <= 126 ? c : ' ');
        }
        return out.toString();
    }
    public static String historyKey(String version) { return "history_v_" + metadata(version); }
    public static boolean matchesProbe(String expected, String actual) {
        return expected != null && !expected.isEmpty() && expected.equals(actual);
    }
    public static String append(String previous, String line) {
        String combined = (previous == null || previous.isEmpty()) ? line : previous + "\n" + line;
        String[] lines = combined.split("\n");
        return String.join("\n", Arrays.copyOfRange(lines, Math.max(0, lines.length - 40), lines.length));
    }
}

