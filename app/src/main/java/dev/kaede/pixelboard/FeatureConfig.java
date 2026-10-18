package dev.kaede.pixelboard;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

/** Snapshot of user-selected feature switches, read once at Gboard process startup. */
public final class FeatureConfig {
    private static final String TAG = "PixelBoardLS";
    private static final Uri URI = Uri.parse("content://dev.kaede.pixelboard.config/flags");
    private static volatile boolean rambler, spellchecker, diagnostics, writingTools;
    private FeatureConfig() {}

    public static void initialize(Context context) {
        rambler = spellchecker = diagnostics = writingTools = false;
        if (context == null) { Log.w(TAG, "Missing context: all switches disabled"); return; }
        try (Cursor c = context.getContentResolver().query(URI, null, null, null, null)) {
            if (c == null || !c.moveToFirst()) return;
            rambler = bool(c, "rambler");
            spellchecker = bool(c, "spellchecker");
            diagnostics = bool(c, "diagnostics");
            writingTools = bool(c, "writing_tools");
            Log.i(TAG, "Feature configuration loaded");
        } catch (RuntimeException e) {
            Log.w(TAG, "Could not read module configuration; features disabled", e);
            rambler = spellchecker = diagnostics = writingTools = false;
        }
    }
    private static boolean bool(Cursor c, String column) {
        int idx = c.getColumnIndex(column);
        return idx >= 0 && c.getInt(idx) == 1;
    }
    public static boolean rambler() { return rambler; }
    public static boolean spellchecker() { return spellchecker; }
    public static boolean diagnostics() { return diagnostics; }
    public static boolean writingTools() { return writingTools; }
}
