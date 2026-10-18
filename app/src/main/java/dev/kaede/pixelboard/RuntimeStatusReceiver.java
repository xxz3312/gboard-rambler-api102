package dev.kaede.pixelboard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Bounded private report. Trusts framework-provided sender identity, never payload identity. */
public final class RuntimeStatusReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT < 34 || !RuntimeStatus.ACTION.equals(intent.getAction())) return;
        String sender = getSentFromPackage();
        int uid = getSentFromUid();
        boolean self = context.getPackageName().equals(sender);
        if (!self && !"com.google.android.inputmethod.latin".equals(sender)
                && !"com.google.android.aicore".equals(sender)
                && !"com.google.android.as.oss".equals(sender)) return;
        try {
            if (sender == null || uid < 0
                    || context.getPackageManager().getApplicationInfo(sender, 0).uid != uid) return;
            String event = intent.getStringExtra("event");
            if (!RuntimeStatusFormat.allowedEvent(event) || (self && !"CHANNEL_TEST".equals(event))) return;
            String detail = RuntimeStatusFormat.metadata(intent.getStringExtra("detail"));
            String version = RuntimeStatusFormat.metadata(intent.getStringExtra("version"));
            int pid = intent.getIntExtra("pid", -1);
            String time = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.US).format(new Date());
            String line = time + " " + sender + " uid=" + uid + " pid=" + pid
                    + " module=" + version + " " + event + " " + detail;
            SharedPreferences prefs = context.getSharedPreferences(RuntimeStatus.PREFS, Context.MODE_PRIVATE);
            if ("RUNTIME_ALIVE".equals(event)) {
                if (!RuntimeStatus.GBOARD.equals(sender) || !RuntimeStatusFormat.matchesProbe(
                        prefs.getString("probe_nonce", ""), intent.getStringExtra("request_id"))) return;
                prefs.edit().putString("probe_reply", line).apply();
            }
            String key = RuntimeStatusFormat.historyKey(version);
            prefs.edit().putString(key, RuntimeStatusFormat.append(prefs.getString(key, ""), line))
                    .putString("last_v_" + version + "_" + sender + "_" + event, line).apply();
        } catch (android.content.pm.PackageManager.NameNotFoundException | RuntimeException ignored) { }
    }
}
