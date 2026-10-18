package dev.kaede.pixelboard;

import android.app.BroadcastOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

/** Read-only live challenge. No model requests or text are inspected. */
public final class RuntimeProbe {
    private static final String PCS = "com.google.android.as.oss";
    private static final String PCS_PING = "dev.kaede.pixelboard.PCS_PING";
    private RuntimeProbe() {}
    public static void installBackend(Context context) {
        if (Build.VERSION.SDK_INT < 34 || !PCS.equals(context.getPackageName())) return;
        try {
            context.registerReceiver(new BroadcastReceiver() {
                @Override public void onReceive(Context host, Intent intent) {
                    if (!isOrderedBroadcast() || !PCS_PING.equals(intent.getAction())
                            || !"dev.kaede.pixelboard".equals(getSentFromPackage())
                            || getSentFromUid() < 0) return;
                    String nonce = intent.getStringExtra("request_id");
                    if (nonce == null || nonce.length() != 36) return;
                    android.os.Bundle reply = new android.os.Bundle();
                    reply.putString("request_id", nonce);
                    reply.putString("events", RuntimeStatus.cachedEvents());
                    reply.putInt("pid", android.os.Process.myPid());
                    setResult(android.app.Activity.RESULT_OK, BuildConfig.VERSION_NAME, reply);
                }
            }, new IntentFilter(PCS_PING), Context.RECEIVER_EXPORTED);
        } catch (RuntimeException | LinkageError e) {
            RuntimeStatus.send(context, "INIT_FAILED", "pcs_live_receiver=" + e.getClass().getSimpleName());
        }
    }
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
                .putString("pcs_probe_reply", "")
                .putLong("probe_time", System.currentTimeMillis()).apply();
        if (Build.VERSION.SDK_INT < 34) return;
        Intent intent = new Intent(RuntimeStatus.PING).setPackage(RuntimeStatus.GBOARD);
        intent.putExtra("request_id", nonce);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        context.sendBroadcast(intent, null,
                BroadcastOptions.makeBasic().setShareIdentityEnabled(true).toBundle());
        requestBackend(context, nonce);
    }

    private static void requestBackend(Context context, String nonce) {
        Intent ping = new Intent(PCS_PING).setPackage(PCS);
        ping.putExtra("request_id", nonce);
        ping.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        Context app = context.getApplicationContext();
        app.sendOrderedBroadcast(ping, null,
                BroadcastOptions.makeBasic().setShareIdentityEnabled(true).toBundle(),
                new BroadcastReceiver() {
                    @Override public void onReceive(Context ignored, Intent intent) {
                        android.content.SharedPreferences prefs = app.getSharedPreferences(RuntimeStatus.PREFS, 0);
                        if (!RuntimeStatusFormat.matchesProbe(prefs.getString("probe_nonce", ""), nonce)
                                || !BuildConfig.VERSION_NAME.equals(prefs.getString("probe_client_version", ""))) return;
                        android.os.Bundle result = getResultExtras(false);
                        String text;
                        if (getResultCode() != android.app.Activity.RESULT_OK || result == null
                                || !nonce.equals(result.getString("request_id"))) {
                            text = "PCS 未应答本次查询；进程可能未启动或查询被系统限制，不能据此判定未注入。";
                        } else if (!BuildConfig.VERSION_NAME.equals(getResultData())) {
                            text = "PCS 应答来自其他模块版本：" + RuntimeStatusFormat.metadata(getResultData())
                                    + "；请重载目标进程。";
                        } else {
                            String events = result.getString("events", "");
                            if (events.length() > 16 * 513) return;
                            text = "PCS 主动查询应答 pid=" + result.getInt("pid", -1)
                                    + " module=" + getResultData()
                                    + "\n以下为该进程缓存的历史事件，时间为事件发生时间；不代表推理成功。\n" + events;
                        }
                        prefs.edit().putString("pcs_probe_reply", text).apply();
                    }
                }, null, 0, null, null);
    }
}

