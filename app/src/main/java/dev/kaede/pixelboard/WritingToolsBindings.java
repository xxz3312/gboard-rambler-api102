package dev.kaede.pixelboard;

/**
 * Method signatures from upstream GboardAiWritingToolsFlagValuePatch (GPL-3.0).
 * These are inventory data, not executable patches. The original route insertion
 * edits bytecode inside the target method and cannot be represented by returning
 * true from an unrelated hook.
 */
public final class WritingToolsBindings {
    private WritingToolsBindings() {}
    public static final TargetMethod AUTO_FIX_ROUTE_1803 =
        new TargetMethod("rzb", "v", "vvw");
    public static final TargetMethod AUTO_FIX_ACCEPTANCE_1803 =
        new TargetMethod("gwf", "a", "lyg", "gwo");
    public static final TargetMethod GEN_AI_INIT_1803 =
        new TargetMethod("ifn", "c", "void",
            "com.google.android.apps.inputmethod.libs.nga.impl.input.NgaInputManager", "hmi");
    public static final TargetMethod SMART_EDIT_INIT_1803 =
        new TargetMethod("ifn", "d", "void");
    public static final TargetMethod CLIENT_REFRESH_1803 =
        new TargetMethod("com.google.android.apps.inputmethod.libs.nga.impl.input.NgaInputManager",
            "e", "void");
    public static TargetMethod[] candidates1803() {
        return new TargetMethod[] {AUTO_FIX_ROUTE_1803, AUTO_FIX_ACCEPTANCE_1803,
            GEN_AI_INIT_1803, SMART_EDIT_INIT_1803, CLIENT_REFRESH_1803};
    }
}
