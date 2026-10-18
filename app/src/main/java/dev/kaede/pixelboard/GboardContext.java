package dev.kaede.pixelboard;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import java.lang.reflect.Method;

/** Best-effort read-only discovery of the current application, guarded by package identity. */
public final class GboardContext {
    private static final String TAG = "PixelBoardLS";
    private static final String TARGET = "com.google.android.inputmethod.latin";
    private GboardContext() {}
    public static Context current() { return current(TARGET); }
    public static Context current(String expectedPackage) {
        try {
            Class<?> cls = Class.forName("android.app.ActivityThread");
            Method current = cls.getDeclaredMethod("currentApplication");
            Object candidate = current.invoke(null);
            if (!(candidate instanceof Application)) return null;
            Context context = ((Application) candidate).getApplicationContext();
            if (context == null) context = (Application) candidate;
            return expectedPackage != null && expectedPackage.equals(context.getPackageName()) ? context : null;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            Log.d(TAG, "Application Context not ready: " + e.getClass().getSimpleName());
            return null;
        }
    }
}
