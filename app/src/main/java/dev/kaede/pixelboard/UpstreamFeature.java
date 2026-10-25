package dev.kaede.pixelboard;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/** Opt-in subset of PixelBoard runtime policies, GPL-3.0. No persistent host writes. */
public enum UpstreamFeature {
    EMOJI_PALETTE("emoji_palette", "表情面板 V2", "启用新版表情选择面板",
            "emoji_palette_v2", "enable_emoji_frequent_recent_switch_option"),
    FREQUENT_EMOJI("frequent_emoji", "常用表情优先", "将最近表情页切换为常用表情；关闭后遵循 Gboard 原设置",
            "enable_frequent_emoji_recent_Tab"),
    EMOJI_SIZE("emoji_size", "表情大小入口", "尝试恢复 Gboard 设置中的表情大小选项",
            "emoji_scale_supported"),
    QUICK_INSERT("quick_insert", "快速插入面板", "尝试启用整合式快捷插入入口",
            "super_insert", "super_insert_vk", "super_insert_enable_dynamic_multi_view",
            "enable_si_shortcut_keys"),
    SMART_COMPOSE("smart_compose", "行内智能补全", "输入时显示补全文字；取决于语言、模型与应用支持",
            "enable_smart_compose", "enable_inline_suggestions_on_all_fields",
            "enable_inline_completion", "enable_delight5_smart_compose"),
    FAST_LEARNING("fast_learning", "加快词汇学习", "降低新词学习阈值并启用历史词汇预测",
            "enable_pc_user_history_ngram_count", "populate_ngram_count_with_context",
            "populate_ngram_timestamp_in_user_history", "enable_multiword_predictions_from_user_history",
            "enable_multiword_suggestions_from_user_history", "enable_dynamic_lm_unigram_continuation_scoring",
            "preserve_context_in_dynamic_lm_compute_history", "enable_dynamic_lm_noise_filter",
            "enable_oov_deduplication"),
    EMOJI_SUGGESTIONS("emoji_suggestions", "输入时联想表情", "根据输入词语显示表情候选；需开启 Gboard 表情建议",
            "config_expression", "enable_concept_prediction_engine", "enable_rule_based_concept_prediction",
            "enable_last_word_rule_based_concept_prediction", "enable_suffix_rule_based_concept_prediction",
            "enable_emoji_suggestion_from_top_candidate", "enable_predictions_from_emoji_shortcuts",
            "enable_ngram_emoji_shortcuts_by_spans", "enable_ngram_emoji_shortcuts_for_prediction",
            "retain_emoji_shortcut_candidates_on_selection"),
    GLIDE_TYPING("glide_typing", "滑行输入增强", "启用手势流与两阶段解码；需语言模型支持",
            "update_streaming_touch_data_set_is_gesture", "enable_two_stage_decoder"),
    AUTO_CORRECTION("auto_correction", "自动纠错增强", "改善空格纠错与候选提交；保留原有应用和语言限制",
            "enable_space_transposition_post_correction", "enable_pk_spatial_model_features",
            "enable_pk_auto_correction_trigger_predictor", "enable_post_correction_trigger_predictor",
            "populate_preemptive_next_key_predictions", "auto_correct_with_replace_text",
            "enable_revert_auto_correct_on_pk", "enable_decoder_response_wait_without_lock",
            "try_initialize_decoder_after_lm_loads"),
    INCOGNITO("incognito_entry", "手动无痕入口", "显示 Gboard 原生无痕模式切换入口",
            "enable_incognito_mode_entry_point", "allow_incognito_mode_switch"),
    EMOGEN("emogen", "AI 表情生成入口", "仅开放原生入口；服务、地区与账号支持仍由 Gboard 决定",
            "enable_emogen", "emogen_entry_point", "emogen_feature_enabled", "emogen_in_access_points"),
    OPAQUE_EMOJI("opaque_emoji", "候选表情完整色彩", "绘制候选表情时恢复不透明色彩；普通文字保持原样");

    public final String key, title, summary;
    private final Map<String, Object> overrides = new HashMap<>();

    UpstreamFeature(String key, String title, String summary, String... flags) {
        this.key = key;
        this.title = title;
        this.summary = summary;
        for (String flag : flags) overrides.put(flag, Boolean.TRUE);
    }

    static {
        AUTO_CORRECTION.overrides.put("delay_to_wait_for_suggestions_before_commit_ms", 150L);
        SMART_COMPOSE.overrides.put("smart_compose_status", 1L);
        // Use the actual upstream runtime values, rather than README's older threshold of 1.
        FAST_LEARNING.overrides.put("intended_oov_min_count", 2L);
        FAST_LEARNING.overrides.put("min_char_num_limit_for_single_word_predictions", 2L);
        FAST_LEARNING.overrides.put("add_pending_oovs_on_reset_max_ms", 500L);
        FAST_LEARNING.overrides.put("add_pending_oovs_on_create_max_ms", 500L);
        FAST_LEARNING.overrides.put("threshold_for_candidates_from_user_history", -5.0d);
    }

    public static boolean targeted(String name) {
        if (name == null) return false;
        for (UpstreamFeature feature : values()) if (feature.overrides.containsKey(name)) return true;
        return false;
    }
    boolean usesFlags() { return !overrides.isEmpty(); }

    static Object transform(String name, Object original, Predicate<UpstreamFeature> enabled) {
        if (name == null || original == null) return original;
        for (UpstreamFeature feature : values()) {
            Object replacement = feature.overrides.get(name);
            if (replacement != null && enabled.test(feature) && replacement.getClass() == original.getClass())
                return replacement;
        }
        return original;
    }

    public static Object transform(String name, Object original) {
        return transform(name, original, FeatureConfig::enabled);
    }
}
