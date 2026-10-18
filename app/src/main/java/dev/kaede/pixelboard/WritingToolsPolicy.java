package dev.kaede.pixelboard;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Read-only, type-safe Writing Tools flag classification.
 * Names sourced from PixelBoard's GboardAiWritingToolsRuntime (GPL-3.0).
 * This class NEVER changes Gboard values; activation requires separately
 * verified runtime method bindings and user-consent semantics.
 */
public final class WritingToolsPolicy {
    private WritingToolsPolicy() {}
    private static final Set<String> BOOLEAN_FLAGS = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(
            "writing_helper", "config_proofread", "writing_helper_on_selected_text",
            "writing_helper_enable_text_stylization_internal", "writing_tools",
            "enable_writing_tools_cooperative_mode", "enable_on_device_proofread",
            "enable_writing_tools_voice_commands", "enable_writing_tools_v2",
            "enable_writing_tools_v2_on_toolbar", "enable_writing_tools_suggest_style",
            "writing_tools_v2", "writing_tools_v2_enable_suggested_instructions",
            "writing_tools_v2_enable_suggested_instructions_in_draft_response",
            "writing_tools_v2_display_instruction_suggestions_in_zero_state",
            "writing_tools_v2_allow_empty_user_instruction",
            "writing_tools_v2_show_predefined_prompts", "enable_writing_tools_my_style",
            "enable_writing_tools_my_style_plus", "enable_nga_lab_modeless_smartedit"
        ))
    );
    private static final Set<String> STRING_FLAGS = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(
            "writing_helper_supported_language_tags", "llm_pc_supported_language_tags",
            "writing_helper_model_version",
            "writing_helper_text_stylization_model_version",
            "writing_tools_v2_backend_type"
        ))
    );
    public enum Type { BOOLEAN, STRING, UNKNOWN }
    public static Type classify(String name) {
        if (name == null) return Type.UNKNOWN;
        if (BOOLEAN_FLAGS.contains(name)) return Type.BOOLEAN;
        if (STRING_FLAGS.contains(name)) return Type.STRING;
        return Type.UNKNOWN;
    }
    public static boolean matchesType(String name, Object value) {
        Type t = classify(name);
        return t == Type.BOOLEAN && value instanceof Boolean
            || t == Type.STRING && value instanceof String;
    }
    public static Object unchanged(String name, Object original) { return original; }
}
