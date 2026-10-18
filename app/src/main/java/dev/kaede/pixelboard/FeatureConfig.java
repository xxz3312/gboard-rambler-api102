package dev.kaede.pixelboard;

/** All behavior-changing features stay off until verified against the installed Gboard. */
public final class FeatureConfig {
    private FeatureConfig() {}
    public static final boolean ENABLE_INLINE_SPELLCHECKER = false;
    public static final boolean ENABLE_RAMBLER = false;
    public static final boolean ENABLE_WRITING_TOOLS = false;
    public static final boolean ENABLE_DIAGNOSTIC_HOOKS = false;
}
