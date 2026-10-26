package dev.kaede.pixelboard;

import android.content.Context;
import android.graphics.Paint;
import android.util.Log;
import android.widget.TextView;
import io.github.libxposed.api.XposedModule;
import java.lang.reflect.Method;

/** Framework contracts and semantic host targets; no obfuscated fields or guessed method names. */
final class RuntimeFeatureHooks {
    private static final String TAG = "PixelBoardLS";
    static void install(XposedModule module, Context context, ClassLoader loader) {
        String preference = "missing", calculation = "missing", candidate = "missing";
        try {
            Method getter = Class.forName("android.app.SharedPreferencesImpl", false, loader)
                    .getDeclaredMethod("getString", String.class, String.class);
            module.hook(getter).intercept(chain -> {
                // Keep original exceptions and type validation before applying a read-only override.
                Object original = chain.proceed();
                return RetentionReadScope.override(chain.getArgs().get(0), original, FeatureConfig.retention());
            });
            preference = "installed(scoped-native-key)";
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            Log.w(TAG, "Clipboard preference contract unavailable", error);
        }
        try {
            RuntimeHookResolver.RetentionTarget target = RuntimeHookResolver.retention(context, loader);
            module.hook(target.method).intercept(chain -> {
                Object original;
                try (RetentionReadScope ignored = new RetentionReadScope()) { original = chain.proceed(); }
                return target.duration ? ClipboardRetention.millis(FeatureConfig.retention(), (Long) original) : original;
            });
            calculation = target.duration ? "installed(semantic-ms)" : "installed(semantic-reader)";
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            calculation = "skipped:"+error.getClass().getSimpleName();
            Log.w(TAG, "No unique retention calculation; retention override inactive", error);
        }
        try {
            Method draw = RuntimeHookResolver.candidateDraw(loader);
            for (String name : new String[]{"setColor", "setAlpha"}) {
                module.hook(Paint.class.getDeclaredMethod(name, int.class)).intercept(chain -> {
                    Object result = chain.proceed();
                    CandidatePaintScope.afterPaintChange(chain.getThisObject());
                    return result;
                });
            }
            module.hook(draw).intercept(chain -> {
                TextView view = (TextView) chain.getThisObject();
                boolean active = FeatureConfig.enabled(UpstreamFeature.OPAQUE_EMOJI)
                        && view != null && EmojiClassifier.isEmojiOnly(view.getText());
                try (CandidatePaintScope ignored = new CandidatePaintScope(active)) { return chain.proceed(); }
            });
            candidate = "installed(scoped-draw)";
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            candidate = "skipped:"+error.getClass().getSimpleName();
            Log.w(TAG, "Candidate widget drawing contract unavailable", error);
        }
        CloudRuntimeStatus.send(context,"CLIPBOARD_SYNC",ClipboardFocusSync.install(module,context,loader));
        CloudRuntimeStatus.send(context, "RUNTIME_HOOKS", "retentionPreference="+preference
                +" retentionCalculation="+calculation+" emoji="+candidate+" config=listener; host caches may need restart");
    }
}
