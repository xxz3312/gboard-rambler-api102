package dev.kaede.pixelboard;

import android.app.BroadcastOptions;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Process;

/** Explicit, asynchronous status delivery with an OS-authenticated sender identity. */
public final class RuntimeStatus {
    public static final String ACTION = "dev.kaede.pixelboard.RUNTIME_STATUS";
    public static final String PREFS = "runtime_status";
    private RuntimeStatus() {}

    public static void send(Context context, String event, String detail) {
        if (context == null || Build.VERSION.SDK_INT < 34 || !RuntimeStatusFormat.allowedEvent(event)) return;
        try {
            Intent intent = new Intent(ACTION).setComponent(new ComponentName(
                    "dev.kaede.pixelboard", "dev.kaede.pixelboard.RuntimeStatusReceiver"));
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            intent.putExtra("event", event);
            intent.putExtra("detail", RuntimeStatusFormat.metadata(detail));
            intent.putExtra("version", BuildConfig.VERSION_NAME);
            intent.putExtra("pid", Process.myPid());
            BroadcastOptions options = BroadcastOptions.makeBasic().setShareIdentityEnabled(true);
            context.sendBroadcast(intent, null, options.toBundle());
        } catch (RuntimeException | LinkageError ignored) {
            // Status delivery must never break or alter keyboard execution.
        }
    }
}
