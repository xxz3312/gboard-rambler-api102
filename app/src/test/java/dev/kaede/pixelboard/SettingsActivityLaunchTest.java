package dev.kaede.pixelboard;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
public final class SettingsActivityLaunchTest {
    @Test public void launchesWithoutFrameworkInLightTheme() { launch(false); }
    @Test public void launchesWithoutFrameworkInDarkTheme() { launch(true); }
    private boolean containsUpdateRow(android.view.ViewGroup parent) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            android.view.View child = parent.getChildAt(i);
            if (child instanceof android.widget.TextView
                    && "检查更新".contentEquals(((android.widget.TextView) child).getText())) return true;
            if (child instanceof android.view.ViewGroup && containsUpdateRow((android.view.ViewGroup) child)) return true;
        }
        return false;
    }
    private void launch(boolean dark) {
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(dark
                ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        try(ActivityController<SettingsActivity> controller=Robolectric.buildActivity(SettingsActivity.class)) {
            SettingsActivity activity=controller.setup().visible().get();
            assertNotNull(activity.findViewById(android.R.id.content));
            assertFalse(activity.isFinishing());
            assertTrue(containsUpdateRow((android.view.ViewGroup) activity.findViewById(android.R.id.content)));
        } finally {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }
}
