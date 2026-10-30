package dev.kaede.pixelboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public final class NativeKeyboardPreferencesTest {
    @Test public void liveOverridesAreScopedReadOnlyAndRestoreNativePreference() {
        Context context = RuntimeEnvironment.getApplication();
        SharedPreferences nativePrefs = PreferenceManager.getDefaultSharedPreferences(context.createDeviceProtectedStorageContext());
        SharedPreferences unrelated = context.getSharedPreferences("unrelated", 0);
        SharedPreferences config = context.getSharedPreferences("test_policy", 0);
        NativeKeyboardPreferences policy = new NativeKeyboardPreferences(context);
        nativePrefs.edit().putBoolean("enable_emojify", false).commit();
        config.edit().clear().putBoolean("magic_emoji", true).commit();
        FeatureConfig.reload(config);
        assertTrue(policy.override(nativePrefs, "enable_emojify", false));
        assertFalse(policy.override(unrelated, "enable_emojify", false));
        assertFalse(policy.override(nativePrefs, "enable_fast_access_bar", false));
        assertFalse(nativePrefs.getBoolean("enable_emojify", true));
        assertFalse(policy.override(nativePrefs, "unknown", false));
        config.edit().clear().commit(); FeatureConfig.reload(config);
        assertFalse(policy.override(nativePrefs, "enable_emojify", false));
        assertTrue(policy.override(nativePrefs, "enable_emojify", true));
        assertFalse(nativePrefs.getBoolean("enable_emojify", true));
    }
}
