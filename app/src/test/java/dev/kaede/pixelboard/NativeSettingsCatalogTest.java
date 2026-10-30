package dev.kaede.pixelboard;

import android.content.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public final class NativeSettingsCatalogTest {
    @Test public void nativeEquivalenceRequiresBothPreferenceKeyAndControlType() {
        Map<String,String> controls=new HashMap<>();
        controls.put("enable_fast_access_bar","PreferenceCategory");
        NativeSettingsCatalog.Result wrong=new NativeSettingsCatalog.Result("a",controls,Set.of());
        assertFalse(wrong.owns(UpstreamFeature.QUICK_EMOJI));
        controls.put("enable_fast_access_bar","SwitchPreferenceCompat");
        controls.put("enable_emojify","custom.AutoSyncedSwitchPreference");
        controls.put("keyboard_emoji_scale_setting","custom.EmojiScaleSliderPreference");
        controls.put("pref_key_latin_enable_ac_revert","SwitchPreferenceCompat");
        controls.put("use_system_font","ListBooleanPreference");
        NativeSettingsCatalog.Result result=new NativeSettingsCatalog.Result("b",controls,Set.of());
        assertEquals(Set.of(UpstreamFeature.QUICK_EMOJI,UpstreamFeature.MAGIC_EMOJI,UpstreamFeature.EMOJI_SIZE),result.owned);
        assertFalse(result.owns(UpstreamFeature.UNDO));
        assertFalse(result.owns(UpstreamFeature.GOOGLE_SANS));
        assertTrue(NativeSettingsCatalog.EMPTY.owned.isEmpty());
    }
    @Test public void staleEnabledModuleValuesCannotOverrideHiddenNativeControls() {
        Context context=RuntimeEnvironment.getApplication();
        android.content.SharedPreferences prefs=context.getSharedPreferences("native_migration",0);
        prefs.edit().clear().putBoolean("quick_emoji",true).putBoolean("magic_emoji",true).putBoolean("undo_button",true).commit();
        FeatureConfig.reload(prefs);
        NativeSettingsCatalog.Result result=new NativeSettingsCatalog.Result("native",
                Map.of("enable_fast_access_bar","SwitchPreferenceCompat","enable_emojify","SwitchPreferenceCompat"),Set.of());
        FeatureConfig.nativeSettings(result);
        try {
            assertFalse(FeatureConfig.enabled(UpstreamFeature.QUICK_EMOJI));
            assertFalse(FeatureConfig.enabled(UpstreamFeature.MAGIC_EMOJI));
            assertTrue(FeatureConfig.enabled(UpstreamFeature.UNDO));
            assertEquals(false,UpstreamFeature.transform("enable_fast_access_bar",false));
            assertEquals(false,UpstreamFeature.transform("enable_emojify",false));
            NativeKeyboardPreferences nativePrefs=new NativeKeyboardPreferences(context);
            android.content.SharedPreferences stock=android.preference.PreferenceManager.getDefaultSharedPreferences(context);
            assertFalse(nativePrefs.override(stock,"enable_fast_access_bar",false));
            assertFalse(nativePrefs.override(stock,"enable_emojify",false));
        } finally { FeatureConfig.nativeSettings(NativeSettingsCatalog.EMPTY); prefs.edit().clear().commit(); FeatureConfig.reload(prefs); }
        assertFalse(FeatureConfig.enabled(UpstreamFeature.QUICK_EMOJI));
    }
    @Test public void shortNativeRetentionRangeDoesNotHideTheExtendedRange() {
        Map<String,String> controls=Map.of(ClipboardRetention.NATIVE_KEY,"custom.LabelSummarySliderPreference");
        assertFalse(new NativeSettingsCatalog.Result("short",controls,Set.of("1","2","3","6","12","18","24")).retention);
        assertTrue(new NativeSettingsCatalog.Result("full",controls,Set.of("1","24","168","720","876000")).retention);
    }
    @Test public void absentGboardKeepsControlsAndXmlPrefilterSupportsBothEncodings() {
        assertTrue(NativeSettingsCatalog.detect(RuntimeEnvironment.getApplication()).owned.isEmpty());
        assertTrue(NativeSettingsCatalog.candidate("SwitchPreferenceCompat".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertTrue(NativeSettingsCatalog.candidate("SwitchPreferenceCompat".getBytes(java.nio.charset.StandardCharsets.UTF_16LE)));
        assertFalse(NativeSettingsCatalog.candidate("LinearLayout".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
}
