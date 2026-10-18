package dev.kaede.pixelboard;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.content.SharedPreferences;
import android.content.UriMatcher;

/** Public READ-ONLY feature flags only; no sensitive data and no mutation API. */
public final class ConfigProvider extends ContentProvider {
    private static final String AUTH = "dev.kaede.pixelboard.config";
    @Override public boolean onCreate() { return true; }
    @Override public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        if (!AUTH.equals(uri.getAuthority()) || !"/flags".equals(uri.getPath())) return null;
        SharedPreferences p = getContext().getSharedPreferences("features", 0);
        String[] columns = {"rambler", "spellchecker", "diagnostics", "writing_tools"};
        MatrixCursor c = new MatrixCursor(columns, 1);
        c.addRow(new Object[]{p.getBoolean("rambler", false) ? 1 : 0,
                p.getBoolean("spellchecker", false) ? 1 : 0,
                p.getBoolean("diagnostics", false) ? 1 : 0,
                p.getBoolean("writing_tools", false) ? 1 : 0});
        return c;
    }
    @Override public String getType(Uri uri) { return null; }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String selection, String[] args) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) { throw new UnsupportedOperationException(); }
}
