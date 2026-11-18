package dev.kaede.pixelboard;

import android.content.Context;
import android.content.Intent;
import android.os.UserManager;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowUserManager;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public final class DirectBootGateTest {
    @Test public void lockedStorageWaitsForUnlockAndRunsExactlyOnce() {
        Context context = RuntimeEnvironment.getApplication();
        UserManager manager = context.getSystemService(UserManager.class);
        Shadow.<ShadowUserManager>extract(manager).setUserUnlocked(false);
        AtomicInteger calls = new AtomicInteger();
        DirectBootGate.whenUnlocked(context, calls::incrementAndGet);
        assertEquals(0, calls.get());
        context.sendBroadcast(new Intent(Intent.ACTION_USER_UNLOCKED));
        ShadowLooper.idleMainLooper();
        assertEquals(0, calls.get());
        Shadow.<ShadowUserManager>extract(manager).setUserUnlocked(true);
        context.sendBroadcast(new Intent(Intent.ACTION_USER_UNLOCKED));
        ShadowLooper.idleMainLooper();
        assertEquals(1, calls.get());
        context.sendBroadcast(new Intent(Intent.ACTION_USER_UNLOCKED));
        ShadowLooper.idleMainLooper();
        assertEquals(1, calls.get());
    }
    @Test public void unlockedStorageInitializesImmediately() {
        Context context = RuntimeEnvironment.getApplication();
        Shadow.<ShadowUserManager>extract(context.getSystemService(UserManager.class)).setUserUnlocked(true);
        AtomicInteger calls = new AtomicInteger();
        DirectBootGate.whenUnlocked(context, calls::incrementAndGet);
        assertEquals(1, calls.get());
    }
}
