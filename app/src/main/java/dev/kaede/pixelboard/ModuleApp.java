package dev.kaede.pixelboard;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;
import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

/** Module-side bridge: copies locally saved switches to LSPosed remote preferences. */
public final class ModuleApp extends Application implements XposedServiceHelper.OnServiceListener {
    private static final String TAG = "PixelBoardLS";
    private static volatile XposedService service;
    private volatile String lastSync = "尚未同步";
    public static boolean connected() { return service != null; }

    @Override public void onCreate() {
        super.onCreate();
        getSharedPreferences("features", MODE_PRIVATE).edit().remove("spellchecker").apply();
        try { XposedServiceHelper.registerListener(this); }
        catch (RuntimeException e) { Log.e(TAG, "LSPosed service listener registration failed", e); }
    }
    @Override public void onServiceBind(XposedService bound) {
        service = bound;
        sync();
    }
    @Override public void onServiceDied(XposedService dead) {
        if (service == dead) service = null;
    }

    public String configurationStatus() {
        SharedPreferences local = getSharedPreferences("features", MODE_PRIVATE);
        return "保存配置：Rambler=" + local.getBoolean("rambler", false)
                + " 写作工具=" + local.getBoolean("writing_tools", false)
                + "\n服务连接：" + connected() + "\n" + lastSync;
    }

    public void sync() {
        XposedService current = service;
        if (current == null) { lastSync = "LSPosed 服务未连接：开关仅保存在本地，尚未同步。"; Log.w(TAG, lastSync); return; }
        try {
            SharedPreferences local = getSharedPreferences(FeatureConfig.GROUP, MODE_PRIVATE);
            SharedPreferences remote = current.getRemotePreferences(FeatureConfig.GROUP);
            boolean success = remote.edit()
                    .putBoolean("rambler", local.getBoolean("rambler", false))
                    .remove("spellchecker")
                    .putBoolean("diagnostics", local.getBoolean("diagnostics", false))
                    .putBoolean("writing_tools", local.getBoolean("writing_tools", false))
                    .commit();
            lastSync = success ? "最近一次同步成功（不代表 Hook 已加载）。" : "配置同步失败。";
            Log.i(TAG, "Remote preferences sync=" + success);
        } catch (RuntimeException e) {
            lastSync = "配置同步异常：" + e.getClass().getSimpleName();
            Log.e(TAG, "Remote preferences sync failed", e);
        }
    }
}

