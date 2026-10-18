package dev.kaede.pixelboard;

import android.content.SharedPreferences;
import android.util.Log;
import io.github.libxposed.api.XposedModule;

/** Read-only framework-backed config in hooked Gboard. No ContentProvider IPC. */
public final class FeatureConfig {
    private static final String TAG = "PixelBoardLS";
    public static final String GROUP = "features";
    private static volatile boolean rambler, spellchecker, diagnostics, writingTools;
    private FeatureConfig() {}
    public static void initialize(XposedModule module) {
        rambler = spellchecker = diagnostics = writingTools = false;
        try {
            SharedPreferences p = module.getRemotePreferences(GROUP);
            rambler = p.getBoolean("rambler", false);
            spellchecker = p.getBoolean("spellchecker", false);
            diagnostics = p.getBoolean("diagnostics", false);
            writingTools = p.getBoolean("writing_tools", false);
            module.log(Log.INFO, TAG, "Remote settings read: rambler=" + rambler
                    + ", diagnostics=" + diagnostics + ", spellchecker=" + spellchecker
                    + ", writingTools=" + writingTools);
        } catch (RuntimeException e) {
            module.log(Log.ERROR, TAG, "Remote settings unavailable; fail closed", e);
        }
    }
    public static boolean rambler() { return rambler; }
    public static boolean spellchecker() { return spellchecker; }
    public static boolean diagnostics() { return diagnostics; }
    public static boolean writingTools() { return writingTools; }
}
