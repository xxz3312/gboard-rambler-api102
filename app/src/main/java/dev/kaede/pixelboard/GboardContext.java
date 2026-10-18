package dev.kaede.pixelboard;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import java.lang.reflect.Method;

/** Best-effort read-only discovery in the already-running Gboard process. */
public final class GboardContext {
    private static final String TAG = "PixelBoardLS";
    private static final String TARGET = "com.google.android.inputmethod.latin";
    private GboardContext() {}
    public static Context current() {
        try {
            Class<?> cls = Class.forName("android.app.ActivityThread");
            Method current = cls.getDeclaredMethod("currentApplication");
            Object candidate = current.invoke(null);
            if (!(candidate instanceof Application)) return null;
            Context context = ((Application) candidate).getApplicationContext();
            if (context == null) context = (Application) candidate;
            return TARGET.equals(context.getPackageName()) ? context : null;
        } catch (ReflectiveOperationException | LinkageError | SecurityException e) {
            Log.d(TAG, "Gboard Context not ready: " + e.getClass().getSimpleName());
            return null;
        }
    }
}
