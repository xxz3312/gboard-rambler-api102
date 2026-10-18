package dev.kaede.pixelboard;

import android.content.Context;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Runtime-only subset of PixelBoard's Writing Tools flag policy (GPL-3.0).
 * Never spoof hardware, consent, model availability or Google service endpoints.
 */
public final class WritingToolsFlagPolicy {
    private WritingToolsFlagPolicy() {}

    private static final Set<String> LEGACY = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(
            "config_proofread",
            "writing_tools",
            "enable_writing_tools_cooperative_mode",
            "writing_helper_on_selected_text",
            "writing_helper_enable_text_stylization_internal",
            "writing_helper",
            "enable_nga_lab_modeless_smartedit"
        )));
    // Runtime-only cloud-capable entry gates from PixelBoard.
    // Don't enable on-device-only styling and Gemini Nano features on Xiaomi.
    private static final Set<String> CLOUD_ENTRY = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(
            "enable_writing_tools_v2",
            "enable_writing_tools_v2_on_toolbar",
            "writing_tools_v2",
            "writing_tools_v2_enable_suggested_instructions",
            "writing_tools_v2_enable_suggested_instructions_in_draft_response",
            "writing_tools_v2_display_instruction_suggestions_in_zero_state",
            "writing_tools_v2_allow_empty_user_instruction",
            "writing_tools_v2_show_predefined_prompts"
        )));
    public static boolean targeted(String name) {
        return name != null && (LEGACY.contains(name) || CLOUD_ENTRY.contains(name)
                || "enable_writing_tools_voice_commands".equals(name)
                || "writing_tools_v2_backend_type".equals(name));
    }
    public static boolean isToolbarFlag(String name) {
        return "enable_writing_tools_v2_on_toolbar".equals(name);
    }
    public static Object transform(String name, Object original, Context context) {
        if (!FeatureConfig.writingTools() || context == null || !targeted(name))
            return original;
        if ("writing_tools_v2_backend_type".equals(name))
            return original instanceof Long ? Long.valueOf(1L) : original;
        if (!(original instanceof Boolean)) return original;
        if (LEGACY.contains(name) || CLOUD_ENTRY.contains(name)
                || "enable_writing_tools_voice_commands".equals(name))
            return Boolean.TRUE;
        // Do not enable device-only features or alter server endpoints, eligibility,
        // user consent, age, AICore or model-download state.
        return original;
    }
}
