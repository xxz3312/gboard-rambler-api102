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
    @Test public void upstreamSwitchesPersistIndependentlyAcrossRelaunch() {
        android.content.Context context = org.robolectric.RuntimeEnvironment.getApplication();
        android.content.SharedPreferences prefs = context.getSharedPreferences("features", 0);
        prefs.edit().clear().commit();
        try {
            try (ActivityController<SettingsActivity> controller = Robolectric.buildActivity(SettingsActivity.class)) {
                SettingsActivity activity = controller.setup().visible().get();
                android.view.View root = activity.findViewById(android.R.id.content);
                for (UpstreamFeature feature : UpstreamFeature.values())
                    assertFalse(findSwitch(root, feature.title).isChecked());
                findSwitch(root, UpstreamFeature.QUICK_INSERT.title).setChecked(true);
                assertTrue(prefs.getBoolean("quick_insert", false));
                assertFalse(prefs.getBoolean("smart_compose", false));
                assertFalse(prefs.getBoolean("writing_tools", false));
                assertFalse(prefs.getBoolean("rambler", false));
            }
            try (ActivityController<SettingsActivity> controller = Robolectric.buildActivity(SettingsActivity.class)) {
                android.view.View root = controller.setup().visible().get().findViewById(android.R.id.content);
                assertTrue(findSwitch(root, UpstreamFeature.QUICK_INSERT.title).isChecked());
                assertFalse(findSwitch(root, UpstreamFeature.SMART_COMPOSE.title).isChecked());
                findSwitch(root, UpstreamFeature.QUICK_INSERT.title).setChecked(false);
                assertFalse(prefs.getBoolean("quick_insert", true));
            }
        } finally { prefs.edit().clear().commit(); }
    }
    private com.google.android.material.materialswitch.MaterialSwitch findSwitch(android.view.View root, String title) {
        if (root instanceof com.google.android.material.materialswitch.MaterialSwitch) {
            com.google.android.material.materialswitch.MaterialSwitch toggle =
                    (com.google.android.material.materialswitch.MaterialSwitch) root;
            if (toggle.getText().toString().startsWith(title + "\n")) return toggle;
        }
        if (root instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                com.google.android.material.materialswitch.MaterialSwitch match = findSwitch(group.getChildAt(i), title);
                if (match != null) return match;
            }
        }
        return null;
    }
    private void launch(boolean dark) {
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(dark
                ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        try(ActivityController<SettingsActivity> controller=Robolectric.buildActivity(SettingsActivity.class)) {
            SettingsActivity activity=controller.setup().visible().get();
            assertNotNull(activity.findViewById(android.R.id.content));
            assertFalse(activity.isFinishing());
        } finally {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }
}
