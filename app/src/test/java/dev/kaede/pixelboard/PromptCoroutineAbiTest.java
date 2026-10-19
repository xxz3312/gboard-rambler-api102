package dev.kaede.pixelboard;

import org.junit.Test;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.CancellationException;
import static org.junit.Assert.*;

/** Guards the exact linkage used by ML Kit Prompt beta4 on its worker thread. */
public class PromptCoroutineAbiTest {
    @Test public void promptCancellationEntryPointExistsOnResolvedRuntime() throws Exception {
        Class<?> job = Class.forName("kotlinx.coroutines.Job");
        Method cancel = job.getDeclaredMethod("cancel$default", job,
                CancellationException.class, int.class, Object.class);
        assertTrue(Modifier.isStatic(cancel.getModifiers()));
        assertEquals(void.class, cancel.getReturnType());
    }
}
