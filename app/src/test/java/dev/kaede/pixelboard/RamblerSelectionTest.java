package dev.kaede.pixelboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public final class RamblerSelectionTest {
    private Context context;
    private SharedPreferences preferences;

    @Before public void reset() {
        context = RuntimeEnvironment.getApplication();
        preferences = PreferenceManager.getDefaultSharedPreferences(context.createDeviceProtectedStorageContext());
        preferences.edit().clear().commit();
        RamblerSelection.observe(true, context);
    }

    @Test public void disabledSelectionKeepsSettingsVisibleWithoutEnablingRuntime() {
        preferences.edit().putBoolean("enable_jetson", false).commit();
        RamblerSelection.observe(false, context);
        assertFalse(RamblerSelection.mayExpose(context));
        RamblerScope.enterVoice();
        try {
            assertTrue(RamblerSelection.mayExpose(context));
            assertFalse(preferences.getBoolean("enable_jetson", true));
            RamblerScope.enterDefault();
            try { assertFalse(RamblerSelection.mayExpose(context)); }
            finally { RamblerScope.exitDefault(); }
        } finally { RamblerScope.exitVoice(); }
        assertFalse(RamblerSelection.mayExpose(context));
    }

    @Test public void reselectingNativeOptionOverridesStaleFalseObservation() {
        preferences.edit().putBoolean("enable_jetson", false).commit();
        RamblerSelection.observe(false, context);
        preferences.edit().putBoolean("enable_jetson", true).commit();
        assertTrue(RamblerSelection.mayExpose(context));
    }

    @Test public void transientFalseDoesNotDisableAbsentSelection() {
        RamblerSelection.observe(false, context);
        assertTrue(RamblerSelection.mayExpose(context));
        assertFalse(RamblerSelection.mayExpose(null));
    }
}
