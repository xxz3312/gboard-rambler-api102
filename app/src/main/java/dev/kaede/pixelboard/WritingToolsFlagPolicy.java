package dev.kaede.pixelboard;

import android.content.Context;
import android.os.Build;
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
    private static final Set<String> V2 = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(
            "enable_writing_tools_v2",
            "enable_writing_tools_v2_on_toolbar",
            "enable_writing_tools_suggest_style",
            "writing_tools_v2",
            "writing_tools_v2_enable_suggested_instructions",
            "writing_tools_v2_enable_suggested_instructions_in_draft_response",
            "writing_tools_v2_display_instruction_suggestions_in_zero_state",
            "writing_tools_v2_allow_empty_user_instruction",
            "writing_tools_v2_show_predefined_prompts",
            "enable_writing_tools_my_style",
            "enable_writing_tools_my_style_plus"
        )));
    public static boolean targeted(String name) {
        return name != null && (LEGACY.contains(name) || V2.contains(name)
                || "enable_writing_tools_voice_commands".equals(name));
    }
    public static Object transform(String name, Object original, Context context) {
        if (!FeatureConfig.writingTools() || context == null
                || !targeted(name) || !(original instanceof Boolean)) return original;
        if (LEGACY.contains(name) || "enable_writing_tools_voice_commands".equals(name))
            return Boolean.TRUE;
        // PixelBoard gates V2 on compatible Google Tensor devices.
        // Preserve stock on other hardware; do NOT force false or spoof AICore.
        if (V2.contains(name) && isPixelEligible()) return Boolean.TRUE;
        return original;
    }
    private static boolean isPixelEligible() {
        if (!"Google".equalsIgnoreCase(Build.MANUFACTURER)) return false;
        String model = Build.MODEL == null ? "" : Build.MODEL;
        if (model.equalsIgnoreCase("Pixel 8 Pro")) return true;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)^Pixel\\s+(\\d+).*").matcher(model);
        if (!m.matches()) return false;
        try { return Integer.parseInt(m.group(1)) >= 9; }
        catch (NumberFormatException ignored) { return false; }
    }
}
