package dev.kaede.pixelboard;

import android.content.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public final class RetentionSemanticsTest {
    private final List<String> anchors=Arrays.asList(RetentionSemantics.OWNER,RetentionSemantics.METHOD,RetentionSemantics.FAILURE);
    private List<String> calls(String owner) {
        return new ArrayList<>(Arrays.asList(owner+"#ofHours[long]:"+owner,owner+"#toMillis[]:long"));
    }
    public static class Renamed {
        public static long changedAgain(Context context) { return 3600000L; }
        public static int wrongType(Context context) { return 1; }
        public static long wrongArgument(String context) { return 1; }
        public long instance(Context context) { return 1; }
    }
    @Test public void acceptsBothDesugaredAndPlatformDurationContracts() {
        assertTrue(RetentionSemantics.matches(anchors,calls("j$.time.Duration")));
        assertTrue(RetentionSemantics.matches(anchors,calls("java.time.Duration")));
    }
    @Test public void rejectsLookalikesTimestampCalculationsAndPartialAnchors() {
        assertFalse(RetentionSemantics.matches(anchors.subList(0,2),calls("j$.time.Duration")));
        assertFalse(RetentionSemantics.matches(anchors,calls("unrelated.Duration")));
        List<String> mixed=calls("java.time.Duration"); mixed.set(1,"j$.time.Duration#toMillis[]:long");
        assertFalse(RetentionSemantics.matches(anchors,mixed));
        for (String clock:Arrays.asList("java.lang.System#currentTimeMillis[]:long",
                "j$.time.Instant#now[]:j$.time.Instant","java.time.Instant#toEpochMilli[]:long",
                "android.os.SystemClock#elapsedRealtime[]:long")) {
            List<String> calls=calls("j$.time.Duration"); calls.add(clock);
            assertFalse(clock,RetentionSemantics.matches(anchors,calls));
        }
    }
    @Test public void acceptsRenamedContextGetterAndRejectsUnsafeSignatures() throws Exception {
        RuntimeHookResolver.validateNativeRetention(Renamed.class.getDeclaredMethod("changedAgain",Context.class));
        for (java.lang.reflect.Method method:Renamed.class.getDeclaredMethods()) {
            if (method.getName().equals("changedAgain")) continue;
            try { RuntimeHookResolver.validateNativeRetention(method); fail(method.toString()); }
            catch (IllegalArgumentException expected) {}
        }
    }
    @Test public void selectionUsesMillisPreservesStockAndDoesNotOverflow() {
        assertEquals(86400000L,ClipboardRetention.millis("stock",86400000L));
        assertEquals(3600000L,ClipboardRetention.millis("1",86400000L));
        assertEquals(604800000L,ClipboardRetention.millis("168",3600000L));
        assertEquals(2592000000L,ClipboardRetention.millis("720",3600000L));
        assertEquals(3153600000000L,ClipboardRetention.millis("876000",3600000L));
    }
}
