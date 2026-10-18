package dev.kaede.pixelboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

/**
 * Runtime eligibility decision extracted from PixelBoard's OfficialSelectionRuntime.
 * GPL-3.0 upstream: Akshayykadam/PixelBoard; adapted for runtime hooks.
 * No preference is written or silently created.
 */
public final class RamblerSelection {
    private static volatile Boolean observedSelection;
    private RamblerSelection() {}

    public static void observe(boolean selected, Context context) {
        if (!selected && !explicitlyDisabled(context)) return;
        observedSelection = selected;
    }

    /** Never re-enable a feature the user explicitly disabled. */
    public static boolean mayExpose(Context context) {
        if (RamblerScope.isDefaultSelection()) return false;
        Boolean saved = readPreference(context);
        if (Boolean.FALSE.equals(saved)) return false;
        if (RamblerScope.isVoiceSettings()) return true;
        Boolean observed = observedSelection;
        return !Boolean.FALSE.equals(observed);
    }

    public static boolean explicitlyDisabled(Context context) {
        return Boolean.FALSE.equals(readPreference(context));
    }

    public static Boolean readPreference(Context context) {
        if (context == null) return null;
        SharedPreferences stock = preferences(context);
        if (stock == null || !stock.contains("enable_jetson")) return null;
        try { return stock.getBoolean("enable_jetson", true); }
        catch (RuntimeException ignored) { return null; }
    }

    private static SharedPreferences preferences(Context context) {
        try {
            Context target = context;
            if (Build.VERSION.SDK_INT >= 24 && !context.isDeviceProtectedStorage()) {
                Context de = context.createDeviceProtectedStorageContext();
                if (de != null) target = de;
            }
            return PreferenceManager.getDefaultSharedPreferences(target);
        } catch (RuntimeException ex) {
            try { return PreferenceManager.getDefaultSharedPreferences(context); }
            catch (RuntimeException ignored) { return null; }
        }
    }
}
