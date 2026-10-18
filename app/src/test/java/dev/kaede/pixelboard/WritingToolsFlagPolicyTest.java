package dev.kaede.pixelboard;

import org.junit.Test;
import static org.junit.Assert.*;

public final class WritingToolsFlagPolicyTest {
    private static final String[] BASIC = {
        "config_proofread", "writing_tools", "writing_tools_enable_stable_entrance", "enable_writing_tools_cooperative_mode",
        "writing_helper_on_selected_text", "writing_helper_enable_text_stylization_internal",
        "writing_helper", "enable_nga_lab_modeless_smartedit", "enable_writing_tools_voice_commands"
    };
    private static final String[] OFF = {
        "enable_writing_tools_v2", "enable_writing_tools_v2_on_toolbar",
        "enable_writing_tools_suggest_style", "enable_writing_tools_my_style",
        "enable_writing_tools_my_style_plus", "writing_tools_v2",
        "writing_tools_v2_enable_suggested_instructions",
        "writing_tools_v2_enable_suggested_instructions_in_draft_response",
        "writing_tools_v2_display_instruction_suggestions_in_zero_state",
        "writing_tools_v2_allow_empty_user_instruction",
        "writing_tools_v2_show_predefined_prompts",
        "writing_tools_enable_hybrid", "enable_on_device_proofread"
    };
    private static final String[] LANGUAGES = {
        "writing_helper_supported_language_tags", "llm_pc_supported_language_tags"
    };
    private Object apply(String flag, Object value) {
        return WritingToolsFlagPolicy.overrideValue(flag, value, true);
    }
    @Test public void basicCloudAndV2AreSeparate() {
        for (String flag : BASIC) assertEquals(flag, Boolean.TRUE, apply(flag, Boolean.FALSE));
        for (String flag : OFF) assertEquals(flag, Boolean.FALSE, apply(flag, Boolean.TRUE));
        assertEquals(Long.valueOf(1), apply("writing_tools_v2_backend_type", Long.valueOf(3)));
        for (String flag : LANGUAGES) assertEquals("*", apply(flag, "en-US"));
    }
    @Test public void regexFallbackPreservesExistingConfiguration() {
        assertEquals("v3", apply("nga_lab_modeless_smartedit_regex_version", ""));
        String existing = new String("v9");
        assertSame(existing, apply("nga_lab_modeless_smartedit_regex_version", existing));
    }
    @Test public void disabledFeatureReturnsOriginalIdentity() {
        String[] extra = {"writing_tools_v2_backend_type",
            "nga_lab_modeless_smartedit_regex_version",
            "writing_helper_supported_language_tags", "llm_pc_supported_language_tags"};
        for (String[] flags : new String[][] {BASIC, OFF, extra})
            for (String flag : flags)
                for (Object original : new Object[] {null, Boolean.TRUE, Boolean.FALSE, Long.valueOf(3), "en-US"})
                    assertSame(flag, original, WritingToolsFlagPolicy.overrideValue(flag, original, false));
    }
    @Test public void wrongTypesAndNullStayUntouched() {
        for (String[] flags : new String[][] {BASIC, OFF})
            for (String flag : flags)
                for (Object original : new Object[] {null, Long.valueOf(0), "false"})
                    assertSame(flag, original, apply(flag, original));
        assertEquals(Integer.valueOf(3), apply("writing_tools_v2_backend_type", Integer.valueOf(3)));
        for (String flag : LANGUAGES) {
            assertSame(Boolean.FALSE, apply(flag, Boolean.FALSE));
            assertNull(apply(flag, null));
        }
    }
    @Test public void unrelatedAndRamblerFlagsStayUntouched() {
        for (String flag : new String[] {null, "unknown", "enable_agentic_dictation",
                "config_agentic_dictation", "ad_activation_type", "show_rambler_dict_settings",
                "enable_writing_tools_for_minors", "writing_helper_server_target"}) {
            assertFalse(WritingToolsFlagPolicy.targeted(flag));
            Object original = new Object();
            assertSame(original, apply(flag, original));
        }
    }

    @Test public void privateBackendsHaveDistinctPoliciesAndCanBeDisabled() {
        for (int backend : new int[] {2, 3}) {
            for (String flag : OFF) {
                boolean expected = !"enable_on_device_proofread".equals(flag) || backend == 2;
                assertEquals(flag, Boolean.valueOf(expected),
                        WritingToolsFlagPolicy.overrideValue(flag, Boolean.FALSE, true, true, backend));
                assertEquals(flag, apply(flag, Boolean.FALSE),
                        WritingToolsFlagPolicy.overrideValue(flag, Boolean.FALSE, true, false, backend));
            }
            assertEquals(Long.valueOf(backend), WritingToolsFlagPolicy.overrideValue(
                    "writing_tools_v2_backend_type", Long.valueOf(1), true, true, backend));
            assertEquals(Long.valueOf(1), WritingToolsFlagPolicy.overrideValue(
                    "writing_tools_v2_backend_type", Long.valueOf(3), true, false, backend));
        }
    }

    @Test public void v2CannotOverrideDisabledFeatureOrUntargetedEligibility() {
        for (int backend : new int[] {2, 3}) {
            for (String[] flags : new String[][] {BASIC, OFF, {"writing_tools_v2_backend_type"}})
                for (String flag : flags)
                    for (Object original : new Object[] {null, Boolean.FALSE, Long.valueOf(1), "original"})
                        assertSame(original, WritingToolsFlagPolicy.overrideValue(
                                flag, original, false, true, backend));
            for (String flag : new String[] {"enable_writing_tools_for_minors", "model_ready",
                    "writing_tools_pi_model_id_override", "writing_tools_pi_aicore_feature_id_override"}) {
                Object original = new Object();
                assertSame(original, WritingToolsFlagPolicy.overrideValue(flag, original, true, true, backend));
            }
            assertEquals(Integer.valueOf(1), WritingToolsFlagPolicy.overrideValue(
                    "writing_tools_v2_backend_type", Integer.valueOf(1), true, true, backend));
            assertEquals("false", WritingToolsFlagPolicy.overrideValue(
                    "enable_writing_tools_v2", "false", true, true, backend));
        }
    }

    @Test public void unknownBackendRetainsBasicCloudPolicy() {
        for (int backend : new int[] {-1, 0, 1, 4, 999}) {
            assertEquals(Long.valueOf(1), WritingToolsFlagPolicy.overrideValue(
                    "writing_tools_v2_backend_type", Long.valueOf(3), true, true, backend));
            for (String flag : OFF)
                assertEquals(Boolean.FALSE, WritingToolsFlagPolicy.overrideValue(
                        flag, Boolean.TRUE, true, true, backend));
        }
    }
}
