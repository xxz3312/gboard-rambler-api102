package dev.kaede.pixelboard;

import org.junit.Test;
import static org.junit.Assert.*;

public class RuntimeEventCacheTest {
    @Test public void retainsProofWhileRepeatedRouteEventsReplaceOneSlot() {
        RuntimeEventCache cache = new RuntimeEventCache();
        cache.put("proof", "original-time proof");
        for (int i = 0; i < 100; i++) cache.put("route", "route-" + i);
        assertEquals("original-time proof\nroute-99", cache.snapshot());
    }
    @Test public void evictsOldTypesAndBoundsPayloads() {
        RuntimeEventCache cache = new RuntimeEventCache();
        for (int i = 0; i < 20; i++) cache.put("e" + i, "x".repeat(600));
        String[] lines = cache.snapshot().split("\n");
        assertEquals(16, lines.length);
        assertEquals(512, lines[0].length());
    }
}
