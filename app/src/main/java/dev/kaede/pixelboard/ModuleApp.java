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
    public static boolean connected() { return service != null; }

    @Override public void onCreate() {
        super.onCreate();
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

    public void sync() {
        XposedService current = service;
        if (current == null) { Log.w(TAG, "LSPosed service not connected; switches saved locally"); return; }
        try {
            SharedPreferences local = getSharedPreferences(FeatureConfig.GROUP, MODE_PRIVATE);
            SharedPreferences remote = current.getRemotePreferences(FeatureConfig.GROUP);
            boolean success = remote.edit()
                    .putBoolean("rambler", local.getBoolean("rambler", false))
                    .putBoolean("spellchecker", local.getBoolean("spellchecker", false))
                    .putBoolean("diagnostics", local.getBoolean("diagnostics", false))
                    .putBoolean("writing_tools", local.getBoolean("writing_tools", false))
                    .putBoolean("writing_tools_v2", local.getBoolean("writing_tools_v2", false))
                    .putInt("writing_tools_backend", local.getInt("writing_tools_backend", 2) == 3 ? 3 : 2)
                    .putBoolean("v2_local_proofread", local.getBoolean("v2_local_proofread", false))
                    .putBoolean("gboard_pixel_identity", local.getBoolean("gboard_pixel_identity", false))
                    .commit();
            Log.i(TAG, "Remote preferences sync=" + success);
        } catch (RuntimeException e) {
            Log.e(TAG, "Remote preferences sync failed", e);
        }
    }
}

