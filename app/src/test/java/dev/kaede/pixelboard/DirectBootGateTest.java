package dev.kaede.pixelboard;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.os.UserManager;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public final class DirectBootGateTest {
    @Test public void lockedStorageWaitsForUnlockAndRunsExactlyOnce() {
        Context context = RuntimeEnvironment.getApplication();
        UserManager manager = context.getSystemService(UserManager.class);
        Shadows.shadowOf(manager).setUserUnlocked(false);
        AtomicInteger calls = new AtomicInteger();
        DirectBootGate.whenUnlocked(context, calls::incrementAndGet);
        assertEquals(0, calls.get());
        context.sendBroadcast(new Intent(Intent.ACTION_USER_UNLOCKED));
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertEquals(0, calls.get());
        Shadows.shadowOf(manager).setUserUnlocked(true);
        context.sendBroadcast(new Intent(Intent.ACTION_USER_UNLOCKED));
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertEquals(1, calls.get());
        context.sendBroadcast(new Intent(Intent.ACTION_USER_UNLOCKED));
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertEquals(1, calls.get());
    }
    @Test public void unlockedStorageInitializesImmediately() {
        Context context = RuntimeEnvironment.getApplication();
        Shadows.shadowOf(context.getSystemService(UserManager.class)).setUserUnlocked(true);
        AtomicInteger calls = new AtomicInteger();
        DirectBootGate.whenUnlocked(context, calls::incrementAndGet);
        assertEquals(1, calls.get());
    }
}
