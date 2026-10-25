package dev.kaede.pixelboard;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public final class DiagnosticLogTest {
    @Test public void historyStaysBoundedAndKeepsLatestFailure() {
        String history="";
        for (int i=0;i<500;i++) history=DiagnosticLog.append(history,"event-"+i+" "+"x".repeat(800));
        history=DiagnosticLog.append(history,"ERROR latest-failure");
        assertTrue(history.length()<=DiagnosticLog.MAX_CHARS);
        assertTrue(history.endsWith("ERROR latest-failure\n"));
        assertFalse(history.contains("event-0 "));
        assertTrue(DiagnosticLog.append("","x".repeat(90000)).length()<2100);
    }
    @Test public void recordsMetadataInPrivateHistory() {
        android.content.Context context=RuntimeEnvironment.getApplication();
        context.getSharedPreferences("diagnostic_history",0).edit().clear().commit();
        DiagnosticLog.record(context,"HOOKS","getterInstalled=true");
        assertTrue(DiagnosticLog.history(context).contains("HOOKS getterInstalled=true"));
        assertFalse(DiagnosticLog.history(context).contains("clipboard contents"));
    }
}
