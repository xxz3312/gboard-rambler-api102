package dev.kaede.pixelboard;

import android.content.SharedPreferences;
import android.util.Log;
import io.github.libxposed.api.XposedModule;

/** Read-only framework-backed config in hooked Gboard. No ContentProvider IPC. */
public final class FeatureConfig {
    private static final String TAG = "PixelBoardLS";
    public static final String GROUP = "features";
    private static volatile boolean rambler, diagnostics, writingTools, scanText, screenshot, roundedKeys;
    private FeatureConfig() {}
    public static void initialize(XposedModule module) {
        rambler = diagnostics = writingTools = scanText = screenshot = roundedKeys = false;
        try {
            SharedPreferences p = module.getRemotePreferences(GROUP);
            rambler = p.getBoolean("rambler", false);
            diagnostics = p.getBoolean("diagnostics", false);
            writingTools = p.getBoolean("writing_tools", false);
            scanText = p.getBoolean("scan_text", false);
            screenshot = p.getBoolean("screenshot_suggestions", false);
            roundedKeys = p.getBoolean("rounded_keys", false);
            module.log(Log.INFO, TAG, "Remote settings read: rambler=" + rambler
                    + ", diagnostics=" + diagnostics
                    + ", writingTools=" + writingTools);
        } catch (RuntimeException e) {
            module.log(Log.ERROR, TAG, "Remote settings unavailable; fail closed", e);
        }
    }
    private static volatile boolean ramblerCompatible, writingCompatible;
    public static boolean scanText() { return scanText; }
    public static boolean screenshot() { return screenshot; }
    public static boolean roundedKeys() { return roundedKeys; }
    public static boolean extrasRequested() { return scanText || screenshot || roundedKeys; }
    public static boolean ramblerRequested() { return rambler; }
    public static boolean writingRequested() { return writingTools; }
    public static void compatibility(boolean voice, boolean writing) {
        ramblerCompatible=voice; writingCompatible=writing;
    }
    public static boolean rambler() { return rambler && ramblerCompatible; }
    public static boolean diagnostics() { return diagnostics; }
    public static boolean writingTools() { return writingTools && writingCompatible; }
}


