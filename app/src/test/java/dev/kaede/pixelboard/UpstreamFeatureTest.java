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

    private Object value(String key, Object original, UpstreamFeature selected) {
        return UpstreamFeature.transform(key, original, feature -> feature == selected);
    }
}
