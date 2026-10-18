package dev.kaede.pixelboard;

import android.content.Context;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Explicit feature allowlist adapted from PixelBoard StockPolicy (GPL-3.0). */
public final class RamblerFlagPolicy {
    private RamblerFlagPolicy() {}
    private static final Set<String> BOOLEAN_FLAGS = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(
            "enable_agentic_dictation",
            "config_agentic_dictation",
            "enable_jetson_in_toolbar",
            "filter_rambler_contributed_input_view_session",
            "enable_rambler_toolbar_at_cursor_position",
            "enable_rambler_al_toolbar",
            "enable_rambler_al_toolbar_overflow_menu",
            "enable_hide_rambler_al_idling_state",
            "enable_rambler_al_show_hide_new_animation",
            "enable_rambler_al_teleporting_animation",
            "rambler_motion_with_gradient_color",
            "agentic_dictation_spot_light2",
            "agentic_dictation_new_gi_motion",
            "enable_jetson_s3_quota_experiment_override",
            "agentic_dictation_disable_pre_session_quota_check",
            "enable_pcm_audio_caching_for_offline_fallback",
            "agentic_dictation_enable_seamless_fallback",
            "agentic_dictation_enable_proactive_parallel_asr"
        ))
    );
    public static boolean targeted(String name) {
        return BOOLEAN_FLAGS.contains(name)
                || "show_rambler_dict_settings".equals(name)
                || "ad_activation_type".equals(name);
    }
    public static Object transform(String name, Object original, Context context) {
        if (!FeatureConfig.rambler() || name == null || context == null) return original;
        // Respect explicit opt-out across all flags, including activation type and settings.
        if (!RamblerSelection.mayExpose(context)) return original;
        if ("show_rambler_dict_settings".equals(name))
            return original instanceof Boolean ? Boolean.TRUE : original;
        if ("ad_activation_type".equals(name))
            return original instanceof Long ? Long.valueOf(2L) : original;
        return BOOLEAN_FLAGS.contains(name) && original instanceof Boolean
                ? Boolean.TRUE : original;
    }
}
