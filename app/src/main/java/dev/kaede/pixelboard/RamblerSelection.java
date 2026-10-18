package dev.kaede.pixelboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

/**
 * Selection policy adapted from Akshayykadam/PixelBoard (GPL-3.0).
 * Reads stock preferences; does not create or modify them.
 */
public final class RamblerSelection {
    private static volatile Boolean observedSelection;
    private RamblerSelection() {}

    public static void observe(boolean selected, Context context) {
        // Do not cache a transient false result as an explicit user opt-out.
        if (!selected && !explicitlyDisabled(context)) return;
        observedSelection = selected;
    }

    public static boolean mayExpose(Context context) {
        if (RamblerScope.isDefaultSelection()) return false;
        if (context == null) return false; // never widen policy without host context
        Boolean pref = readPreference(context);
        if (Boolean.FALSE.equals(pref)) return false;
        if (RamblerScope.isVoiceSettings()) return true;
        return !Boolean.FALSE.equals(observedSelection);
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
