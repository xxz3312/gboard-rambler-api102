package dev.kaede.pixelboard;

import org.junit.Test;
import static org.junit.Assert.*;

public final class RuntimeStatusFormatTest {
    @Test public void unknownEventsAreRejected() {
        assertTrue(RuntimeStatusFormat.allowedEvent("V2_SESSION_FAILED"));
        assertFalse(RuntimeStatusFormat.allowedEvent(null));
        assertFalse(RuntimeStatusFormat.allowedEvent("input_text"));
        assertFalse(RuntimeStatusFormat.allowedEvent("MODE\nV2_ROUTE"));
    }
    @Test public void metadataCannotCreateExtraReportLines() {
        assertEquals("FEATURE_NOT_FOUND   code=606", RuntimeStatusFormat.metadata("FEATURE_NOT_FOUND\n\r code=606"));
        assertEquals(320, RuntimeStatusFormat.metadata(new String(new char[800]).replace('\0', 'x')).length());
        assertEquals("", RuntimeStatusFormat.metadata(null));
    }
    @Test public void reportRetainsTheLastFortyEvents() {
        String report = "";
        for (int i = 0; i < 100; i++) report = RuntimeStatusFormat.append(report, "event=" + i);
        String[] lines = report.split("\n");
        assertEquals(40, lines.length);
        assertEquals("event=60", lines[0]);
        assertEquals("event=99", lines[39]);
    }
    @org.junit.Test public void liveChallengeRejectsMissingAndStaleReplies() {
        org.junit.Assert.assertFalse(RuntimeStatusFormat.matchesProbe("", ""));
        org.junit.Assert.assertFalse(RuntimeStatusFormat.matchesProbe("new", "old"));
        org.junit.Assert.assertTrue(RuntimeStatusFormat.matchesProbe("new", "new"));
    }
    @org.junit.Test public void differentModuleVersionsHaveSeparateHistory() {
        org.junit.Assert.assertNotEquals(RuntimeStatusFormat.historyKey("0.1.11"),
                RuntimeStatusFormat.historyKey("0.1.12"));
    }
}
