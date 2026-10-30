package dev.kaede.pixelboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Read-only overrides limited to Gboard's default preference instances. */
final class NativeKeyboardPreferences {
    private final Set<SharedPreferences> defaults = Collections.newSetFromMap(new IdentityHashMap<>());

    NativeKeyboardPreferences(Context context) {
        add(context);
        add(context.createDeviceProtectedStorageContext());
    }

    private void add(Context context) {
        if (context != null) defaults.add(PreferenceManager.getDefaultSharedPreferences(context));
    }

    boolean override(Object receiver, String key, boolean original) {
        if (!defaults.contains(receiver)) return original;
        if ("enable_rounded_keys".equals(key) && FeatureConfig.roundedKeys()) return true;
        if ("enable_fast_access_bar".equals(key) && FeatureConfig.enabled(UpstreamFeature.QUICK_EMOJI)) return true;
        if ("enable_emojify".equals(key) && FeatureConfig.enabled(UpstreamFeature.MAGIC_EMOJI)) return true;
        if ("spellchecker_enable_chips_ui".equals(key) && FeatureConfig.enabled(UpstreamFeature.PROOFREAD_CHIPS)) return true;
        return original;
    }
}
