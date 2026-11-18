package dev.kaede.pixelboard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserManager;
import android.util.Log;
import java.util.concurrent.atomic.AtomicBoolean;

/** Defer credential-protected preference scanning until the user has unlocked. */
final class DirectBootGate {
    static void whenUnlocked(Context context, Runnable action) {
        if (unlocked(context)) { action.run(); return; }
        AtomicBoolean started = new AtomicBoolean();
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override public void onReceive(Context ctx, Intent intent) {
                if (!Intent.ACTION_USER_UNLOCKED.equals(intent.getAction()) || !unlocked(context)
                        || !started.compareAndSet(false, true)) return;
                try { context.unregisterReceiver(this); } catch (RuntimeException ignored) {}
                action.run();
            }
        };
        try {
            context.registerReceiver(receiver, new IntentFilter(Intent.ACTION_USER_UNLOCKED));
            // Cover an unlock between the initial check and registration.
            if (unlocked(context) && started.compareAndSet(false, true)) {
                context.unregisterReceiver(receiver);
                action.run();
            }
        } catch (RuntimeException error) {
            Log.w("PixelBoardLS", "Cannot defer initialization until unlock; keeping stock behavior", error);
        }
    }
    private static boolean unlocked(Context context) {
        UserManager manager = context.getSystemService(UserManager.class);
        return manager != null && manager.isUserUnlocked();
    }
}
