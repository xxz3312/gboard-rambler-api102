package dev.kaede.pixelboard;

import org.junit.Test;
import static org.junit.Assert.*;

public final class UpstreamFeatureTest {
    @Test public void optInDoesNotEnableOtherGroups() {
        assertEquals(true, value("emoji_palette_v2", false, UpstreamFeature.EMOJI_PALETTE));
        assertEquals(false, value("enable_smart_compose", false, UpstreamFeature.EMOJI_PALETTE));
        assertEquals(false, value("super_insert", false, UpstreamFeature.EMOJI_PALETTE));
        assertEquals(false, value("enable_frequent_emoji_recent_Tab", false, UpstreamFeature.EMOJI_PALETTE));
        assertEquals(true, value("enable_frequent_emoji_recent_Tab", false, UpstreamFeature.FREQUENT_EMOJI));
    }

    @Test public void disablingRestoresBothTrueAndFalseStockValues() {
        for (String key : new String[]{"emoji_palette_v2", "super_insert", "enable_smart_compose",
                "emoji_scale_supported", "enable_frequent_emoji_recent_Tab", "config_expression"}) {
            assertEquals(true, UpstreamFeature.transform(key, true, feature -> false));
            assertEquals(false, UpstreamFeature.transform(key, false, feature -> false));
        }
        assertEquals(4L, UpstreamFeature.transform("intended_oov_min_count", 4L, feature -> false));
    }

    @Test public void numericOverridesPreserveHostTypes() {
        assertEquals(1L, value("smart_compose_status", 0L, UpstreamFeature.SMART_COMPOSE));
        assertEquals(2L, value("intended_oov_min_count", 4L, UpstreamFeature.FAST_LEARNING));
        assertEquals(500L, value("add_pending_oovs_on_create_max_ms", 1000L, UpstreamFeature.FAST_LEARNING));
        assertEquals(-5.0d, value("threshold_for_candidates_from_user_history", -2.0d, UpstreamFeature.FAST_LEARNING));
        assertEquals(4, value("intended_oov_min_count", 4, UpstreamFeature.FAST_LEARNING));
        assertEquals("proto", value("config_expression", "proto", UpstreamFeature.EMOJI_SUGGESTIONS));
        assertNull(UpstreamFeature.transform("super_insert", null, feature -> true));
    }

    @Test public void unrelatedGatesAndPersonalContextSourcesAreUntouched() {
        for (String key : new String[]{"ocr_password_allowlist", "writing_tools_v2", "enable_jetson",
                "super_insert_browsing_history_results", "super_insert_nesta_results",
                "apps_to_respect_no_auto_correction", "enable_pk_auto_correction_locales"}) {
            assertFalse(UpstreamFeature.targeted(key));
            assertEquals(false, UpstreamFeature.transform(key, false, feature -> true));
        }
        assertFalse(UpstreamFeature.targeted(null));
    }

    @Test public void new133GroupsRespectTypesAndRestoreNativeValuesWhenDisabled() {
        assertEquals(true, value("google_sans_flex", false, UpstreamFeature.GOOGLE_SANS));
        assertEquals(false, value("undo_stop_pk_handling", true, UpstreamFeature.UNDO));
        assertEquals(100L, value("undo_max_chunk", 3L, UpstreamFeature.UNDO));
        assertEquals(3, value("undo_max_chunk", 3, UpstreamFeature.UNDO));
        assertEquals("", value("undo_suppress_app_pattern", "exclude", UpstreamFeature.UNDO));
        assertEquals("*", value("fast_access_bar_app_whitelist", "app", UpstreamFeature.QUICK_EMOJI));
        assertEquals("1,2,3", value("emojify_enabled_patterns_in_order", "2", UpstreamFeature.MAGIC_EMOJI));
        assertEquals(false, value("enable_emojify_model", true, UpstreamFeature.MAGIC_EMOJI));
        assertEquals(true, value("enable_spellchecker_chips_ui", false, UpstreamFeature.PROOFREAD_CHIPS));
        assertEquals(false, value("enable_emojify", false, UpstreamFeature.QUICK_EMOJI));
        assertEquals("app", value("fast_access_bar_app_whitelist", "app", UpstreamFeature.MAGIC_EMOJI));
        for (String key : new String[]{"google_sans_flex", "undo_stop_pk_handling", "enable_emojify_model", "enable_spellchecker_chips_ui"}) {
            assertEquals(true, UpstreamFeature.transform(key, true, f -> false));
            assertEquals(false, UpstreamFeature.transform(key, false, f -> false));
        }
        assertEquals(3L, UpstreamFeature.transform("undo_max_chunk", 3L, f -> false));
        assertEquals("app", UpstreamFeature.transform("fast_access_bar_app_whitelist", "app", f -> false));
    }

    private Object value(String key, Object original, UpstreamFeature selected) {
        return UpstreamFeature.transform(key, original, feature -> feature == selected);
    }
}
