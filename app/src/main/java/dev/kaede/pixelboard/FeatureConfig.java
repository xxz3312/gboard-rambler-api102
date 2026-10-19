package dev.kaede.pixelboard;

import android.content.SharedPreferences;
import android.util.Log;
import io.github.libxposed.api.XposedModule;

/** Read-only framework-backed config in hooked target processes. No ContentProvider IPC. */
public final class FeatureConfig {
    private static final String TAG = "PixelBoardLS";
    public static final String GROUP = "features";
    private static volatile boolean rambler, spellchecker, diagnostics, writingTools, writingToolsV2, v2LocalProofread;
    private static volatile int writingToolsBackend = 2;
    private static volatile boolean gboardPixelIdentity;
    private static volatile boolean localModelPreparation;
    private FeatureConfig() {}
    public static void initialize(XposedModule module) {
        rambler = spellchecker = diagnostics = writingTools = writingToolsV2 = v2LocalProofread = false;
        writingToolsBackend = 2;
        gboardPixelIdentity = false;
        localModelPreparation = false;
        try {
            SharedPreferences p = module.getRemotePreferences(GROUP);
            rambler = p.getBoolean("rambler", false);
            spellchecker = p.getBoolean("spellchecker", false);
            diagnostics = p.getBoolean("diagnostics", false);
            writingTools = p.getBoolean("writing_tools", false);
            writingToolsV2 = writingTools && p.getBoolean("writing_tools_v2", false);
            gboardPixelIdentity = writingToolsV2 && p.getBoolean("gboard_pixel_identity", false);
            v2LocalProofread = p.getBoolean("v2_local_proofread", false);
            localModelPreparation = p.getBoolean("local_model_preparation", false);
            writingToolsBackend = p.getInt("writing_tools_backend", 2) == 3 ? 3 : 2;
            module.log(Log.INFO, TAG, "Remote settings read: rambler=" + rambler
                    + ", diagnostics=" + diagnostics + ", spellchecker=" + spellchecker
                    + ", writingTools=" + writingTools + ", v2Requested=" + writingToolsV2
                    + ", requestedBackend=" + writingToolsBackend + ", localProofreadRequested=" + v2LocalProofread);
        } catch (RuntimeException e) {
            writingToolsV2 = false;
            gboardPixelIdentity = false;
            module.log(Log.ERROR, TAG, "Remote settings unavailable; fail closed", e);
        }
    }
    public static boolean localModelPreparation() { return localModelPreparation; }
    public static boolean rambler() { return rambler; }
    public static boolean spellchecker() { return spellchecker; }
    public static boolean diagnostics() { return diagnostics; }
    public static boolean writingTools() { return writingTools; }
    public static boolean writingToolsV2() { return writingToolsV2; }
    public static boolean v2LocalProofread() { return v2LocalProofread; }
    public static int writingToolsBackend() { return writingToolsBackend; }
    public static boolean gboardPixelIdentity() { return gboardPixelIdentity; }
}

