package dev.kaede.pixelboard;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public final class BackendStartupTest {
    @Test public void unrelatedApplicationCannotConsumeInitialization() {
        BackendStartup startup = new BackendStartup();
        AtomicInteger count = new AtomicInteger();
        assertFalse(startup.start("pcs", "keyboard", count::incrementAndGet));
        assertTrue(startup.start("pcs", "pcs", count::incrementAndGet));
        assertEquals(1, count.get());
    }
    @Test public void earlyAndLateCallbacksInstallOnce() {
        BackendStartup startup = new BackendStartup();
        AtomicInteger count = new AtomicInteger();
        assertTrue(startup.start("pcs", "pcs", count::incrementAndGet));
        assertFalse(startup.start("pcs", "pcs", count::incrementAndGet));
        assertFalse(startup.start("pcs", "pcs", count::incrementAndGet));
        assertEquals(1, count.get());
    }
    @Test public void concurrentCallbacksCannotDuplicateHooks() throws Exception {
        BackendStartup startup = new BackendStartup();
        AtomicInteger count = new AtomicInteger();
        CountDownLatch ready = new CountDownLatch(1);
        Thread[] threads = new Thread[8];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                try { ready.await(); startup.start("pcs", "pcs", count::incrementAndGet); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
            threads[i].start();
        }
        ready.countDown();
        for (Thread thread : threads) thread.join();
        assertEquals(1, count.get());
    }
}
