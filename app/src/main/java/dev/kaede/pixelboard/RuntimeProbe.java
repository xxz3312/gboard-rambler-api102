package dev.kaede.pixelboard;

import android.app.BroadcastOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

/** Read-only live challenge. No model requests or text are inspected. */
public final class RuntimeProbe {
    private RuntimeProbe() {}
    public static void install(Context context, ClassLoader loader) {
        if (Build.VERSION.SDK_INT < 34) return;
        try {
            context.registerReceiver(new BroadcastReceiver() {
                @Override public void onReceive(Context host, Intent intent) {
                    if (!RuntimeStatus.PING.equals(intent.getAction())
                            || !"dev.kaede.pixelboard".equals(getSentFromPackage())
                            || getSentFromUid() < 0) return;
                    String nonce = intent.getStringExtra("request_id");
                    if (nonce == null || nonce.length() != 36) return;
                    RuntimeStatus.send(context, "RUNTIME_ALIVE",
                            "v2=" + FeatureConfig.writingToolsV2()
                            + " backend=" + FeatureConfig.writingToolsBackend()
                            + " " + WritingToolsHandlerSnapshot.read(loader), nonce);
                }
            }, new IntentFilter(RuntimeStatus.PING), Context.RECEIVER_EXPORTED);
        } catch (RuntimeException | LinkageError e) {
            RuntimeStatus.send(context, "INIT_FAILED", "live_receiver=" + e.getClass().getSimpleName());
        }
    }
    public static void request(Context context) {
        android.content.SharedPreferences prefs = context.getSharedPreferences(RuntimeStatus.PREFS, 0);
        String nonce = java.util.UUID.randomUUID().toString();
        prefs.edit().putString("probe_client_version", BuildConfig.VERSION_NAME)
                .putString("probe_nonce", nonce).putString("probe_reply", "")
                .putLong("probe_time", System.currentTimeMillis()).apply();
        if (Build.VERSION.SDK_INT < 34) return;
        Intent intent = new Intent(RuntimeStatus.PING).setPackage(RuntimeStatus.GBOARD);
        intent.putExtra("request_id", nonce);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        context.sendBroadcast(intent, null,
                BroadcastOptions.makeBasic().setShareIdentityEnabled(true).toBundle());
    }
}
