package dev.kaede.pixelboard;

import java.util.*;

/** Semantic evidence from the native duration getter; no version or obfuscated-name allowlist. */
final class RetentionSemantics {
    static final String OWNER="com/google/android/apps/inputmethod/libs/clipboard/ClipboardContentProviderUtils";
    static final String METHOD="getUnpinnedItemTimeLimitInMilliSeconds";
    static final String FAILURE="Failed to parse retention hours";
    static boolean matches(Collection<String> strings, Collection<String> calls) {
        if (!strings.containsAll(Arrays.asList(OWNER,METHOD,FAILURE))) return false;
        for (String call:calls) {
            if (call.equals("java.lang.System#currentTimeMillis[]:long")
                    || call.startsWith("android.os.SystemClock#")
                    || call.startsWith("java.time.Instant#") || call.startsWith("j$.time.Instant#")) return false;
        }
        for (String duration:Arrays.asList("java.time.Duration","j$.time.Duration"))
            if (calls.contains(duration+"#ofHours[long]:"+duration)
                    && calls.contains(duration+"#toMillis[]:long")) return true;
        return false;
    }
}
