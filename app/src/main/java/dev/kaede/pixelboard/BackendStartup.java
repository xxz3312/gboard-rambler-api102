package dev.kaede.pixelboard;

import java.util.concurrent.atomic.AtomicBoolean;

/** Package-check before claiming initialization, shared by early and late callbacks. */
public final class BackendStartup {
    private final AtomicBoolean started = new AtomicBoolean();
    public boolean start(String expectedPackage, String actualPackage, Runnable initializer) {
        if (expectedPackage == null || !expectedPackage.equals(actualPackage)
                || !started.compareAndSet(false, true)) return false;
        initializer.run();
        return true;
    }
}
