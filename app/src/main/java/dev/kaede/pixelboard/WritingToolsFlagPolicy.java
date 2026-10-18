package dev.kaede.pixelboard;

import android.content.Context;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Basic cloud Writing Tools policy adapted from Akshayykadam/PixelBoard (GPL-3.0).
 * Never spoof hardware, consent, model availability or Google service endpoints.
 */
public final class WritingToolsFlagPolicy {
    private WritingToolsFlagPolicy() {}

    private static final Set<String> LEGACY = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(
            "config_proofread",
            "writing_tools",
            "writing_tools_enable_stable_entrance",
            "enable_writing_tools_cooperative_mode",
            "writing_helper_on_selected_text",
            "writing_helper_enable_text_stylization_internal",
            "writing_helper",
            "enable_nga_lab_modeless_smartedit"
        )));
    // V2 is independently opt-in; the default remains the tested basic cloud policy.
    private static final Set<String> V2_FLAGS = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(
            "enable_writing_tools_v2",
            "enable_writing_tools_v2_on_toolbar",
            "enable_writing_tools_suggest_style",
            "enable_writing_tools_my_style",
            "enable_writing_tools_my_style_plus",
            "writing_tools_v2",
            "writing_tools_v2_enable_suggested_instructions",
            "writing_tools_v2_enable_suggested_instructions_in_draft_response",
            "writing_tools_v2_display_instruction_suggestions_in_zero_state",
            "writing_tools_v2_allow_empty_user_instruction",
            "writing_tools_v2_show_predefined_prompts"
        )));
    private static final Set<String> LANGUAGE_FLAGS = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(
            "writing_helper_supported_language_tags", "llm_pc_supported_language_tags"
        )));
    public static boolean targeted(String name) {
        return name != null && (LEGACY.contains(name) || V2_FLAGS.contains(name)
                || LANGUAGE_FLAGS.contains(name)
                || "writing_tools_enable_hybrid".equals(name)
                || "enable_on_device_proofread".equals(name)
                || "enable_writing_tools_voice_commands".equals(name)
                || "nga_lab_modeless_smartedit_regex_version".equals(name)
                || "writing_tools_v2_backend_type".equals(name));
    }
    public static boolean isToolbarFlag(String name) {
        return "enable_writing_tools_v2_on_toolbar".equals(name);
    }
    public static Object transform(String name, Object original, Context context) {
        return overrideValue(name, original, context != null && FeatureConfig.writingTools());
    }

    // Pure typed policy, also exercised by host-side regression tests.
    static Object overrideValue(String name, Object original, boolean enabled) {
        return overrideValue(name, original, enabled, false, 2);
    }

    static Object overrideValue(String name, Object original, boolean enabled,
            boolean v2VerifiedAndEnabled, int requestedBackend) {
        return overrideValue(name, original, enabled, v2VerifiedAndEnabled, requestedBackend, false);
    }

    static Object overrideValue(String name, Object original, boolean enabled,
            boolean v2VerifiedAndEnabled, int requestedBackend, boolean localProofread) {
        if (!enabled || !targeted(name)) return original;
        boolean v2 = v2VerifiedAndEnabled && (requestedBackend == 2 || requestedBackend == 3);
        if ("writing_tools_v2_backend_type".equals(name))
            return original instanceof Long ? Long.valueOf(v2 ? requestedBackend : 1L) : original;
        if (LANGUAGE_FLAGS.contains(name))
            return original instanceof String ? "*" : original;
        if ("nga_lab_modeless_smartedit_regex_version".equals(name))
            return original instanceof String && ((String) original).isEmpty() ? "v3" : original;
        if (!(original instanceof Boolean)) return original;
        if (V2_FLAGS.contains(name) || "writing_tools_enable_hybrid".equals(name))
            return Boolean.valueOf(v2);
        if ("enable_on_device_proofread".equals(name))
            return Boolean.valueOf(v2 && requestedBackend == 2 && localProofread);
        if (LEGACY.contains(name) || "enable_writing_tools_voice_commands".equals(name))
            return Boolean.TRUE;
        return original;
    }
}
