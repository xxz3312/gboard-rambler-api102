package dev.kaede.pixelboard;

import android.content.*;
import android.inputmethodservice.InputMethodService;
import android.os.SystemClock;
import android.text.InputType;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import io.github.libxposed.api.XposedModule;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.*;

/** Replay only Gboard's live registered callbacks after foreground input starts. No clipboard reads. */
final class ClipboardFocusSync {
    private final List<WeakReference<ClipboardManager.OnPrimaryClipChangedListener>> listeners=new ArrayList<>();
    private long lastSync=-2000;
    synchronized void add(ClipboardManager.OnPrimaryClipChangedListener listener) {
        listeners.removeIf(ref -> ref.get()==null);
        for (WeakReference<ClipboardManager.OnPrimaryClipChangedListener> ref:listeners) if (ref.get()==listener) return;
        if (listeners.size()<32) listeners.add(new WeakReference<>(listener));
    }
    synchronized void remove(Object listener) { listeners.removeIf(ref -> ref.get()==null || ref.get()==listener); }
    static boolean eligible(EditorInfo editor) {
        if (editor==null || editor.inputType==InputType.TYPE_NULL
                || (editor.imeOptions&EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING)!=0) return false;
        int type=editor.inputType&InputType.TYPE_MASK_CLASS;
        int variation=editor.inputType&InputType.TYPE_MASK_VARIATION;
        return !(type==InputType.TYPE_CLASS_TEXT && (variation==InputType.TYPE_TEXT_VARIATION_PASSWORD
                || variation==InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                || variation==InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD))
                && !(type==InputType.TYPE_CLASS_NUMBER && variation==InputType.TYPE_NUMBER_VARIATION_PASSWORD);
    }
    int dispatch(EditorInfo editor, long now) {
        if (!eligible(editor)) return 0;
        List<ClipboardManager.OnPrimaryClipChangedListener> current=new ArrayList<>();
        synchronized (this) {
            if (now-lastSync<1500) return 0;
            lastSync=now;
            listeners.removeIf(ref -> ref.get()==null);
            for (WeakReference<ClipboardManager.OnPrimaryClipChangedListener> ref:listeners) {
                ClipboardManager.OnPrimaryClipChangedListener callback=ref.get(); if (callback!=null) current.add(callback);
            }
        }
        int count=0;
        for (ClipboardManager.OnPrimaryClipChangedListener callback:current) try {
            callback.onPrimaryClipChanged(); count++;
        } catch (RuntimeException error) { Log.w("PixelBoardLS","Native clipboard callback failed",error); }
        return count;
    }
    static String install(XposedModule module, Context context, ClassLoader loader) {
        try {
            Set<Method> starts=new HashSet<>();
            android.content.pm.ServiceInfo[] services=context.getPackageManager()
                    .getPackageInfo(context.getPackageName(),android.content.pm.PackageManager.GET_SERVICES).services;
            if (services!=null) for (android.content.pm.ServiceInfo info:services) {
                Class<?> cls=Class.forName(info.name,false,loader);
                if (InputMethodService.class.isAssignableFrom(cls)) starts.add(cls.getMethod("onStartInputView",EditorInfo.class,boolean.class));
            }
            Method start=GboardResolver.unique(new ArrayList<>(starts));
            ClipboardFocusSync registry=new ClipboardFocusSync();
            module.hook(ClipboardManager.class.getDeclaredMethod("addPrimaryClipChangedListener",
                    ClipboardManager.OnPrimaryClipChangedListener.class)).intercept(chain -> {
                Object result=chain.proceed();
                registry.add((ClipboardManager.OnPrimaryClipChangedListener)chain.getArgs().get(0)); return result;
            });
            module.hook(ClipboardManager.class.getDeclaredMethod("removePrimaryClipChangedListener",
                    ClipboardManager.OnPrimaryClipChangedListener.class)).intercept(chain -> {
                Object result=chain.proceed(); registry.remove(chain.getArgs().get(0)); return result;
            });
            module.hook(start).intercept(chain -> {
                Object result=chain.proceed();
                if (FeatureConfig.clipboardSync()) {
                    int count=registry.dispatch((EditorInfo)chain.getArgs().get(0),SystemClock.elapsedRealtime());
                    if (FeatureConfig.diagnostics()) CloudRuntimeStatus.send(context,"CLIPBOARD_SYNC","callbacks="+count);
                }
                return result;
            });
            return "installed(android-listener/ime-lifecycle)";
        } catch (Exception | LinkageError error) {
            Log.w("PixelBoardLS","Clipboard focus sync unavailable",error);
            return "skipped:"+error.getClass().getSimpleName();
        }
    }
}
