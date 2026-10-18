package dev.kaede.pixelboard;
import org.junit.Test;
import static org.junit.Assert.*;

public final class RuntimeErrorMetadataTest {
    @Test public void messagesAndTextAreNeverIncluded() {
        Throwable failure = new IllegalStateException("secret input",
                new java.io.IOException("secret token"));
        String result = RuntimeErrorMetadata.describe(failure);
        assertEquals("causes=java.lang.IllegalStateException>java.io.IOException", result);
        assertFalse(result.contains("secret"));
    }
    @Test public void cyclesAreBounded() {
        Throwable first = new RuntimeException();
        Throwable second = new IllegalArgumentException();
        first.initCause(second);
        second.initCause(first);
        assertEquals("causes=java.lang.RuntimeException>java.lang.IllegalArgumentException>...",
                RuntimeErrorMetadata.describe(first));
    }
    @Test public void deepCausesAreBounded() {
        Throwable failure = null;
        for (int i = 0; i < 20; i++) failure = new RuntimeException("private", failure);
        assertEquals(4, RuntimeErrorMetadata.describe(failure).split("java.lang.RuntimeException", -1).length - 1);
    }
}
