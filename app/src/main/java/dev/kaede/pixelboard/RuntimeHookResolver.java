package dev.kaede.pixelboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.widget.TextView;
import java.lang.reflect.*;
import java.util.*;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

/** Optional targets resolve independently. Missing/ambiguous matches never choose the first method. */
final class RuntimeHookResolver {
    static Method candidateDraw(ClassLoader loader) throws ReflectiveOperationException {
        // This public widget name and the Android callback are stable across obfuscation mappings.
        Class<?> cls = Class.forName(
                "com.google.android.libraries.inputmethod.widgets.text.AutoCenterScaleTextView", false, loader);
        Method method = cls.getDeclaredMethod("onDraw", Canvas.class);
        validateDraw(method);
        method.setAccessible(true);
        return method;
    }
    static void validateDraw(Method method) {
        if (!TextView.class.isAssignableFrom(method.getDeclaringClass())
                || !method.getName().equals("onDraw") || method.getReturnType()!=void.class
                || !Arrays.equals(method.getParameterTypes(), new Class<?>[]{Canvas.class})
                || Modifier.isStatic(method.getModifiers()) || Modifier.isAbstract(method.getModifiers()))
            throw new IllegalArgumentException("Not a TextView drawing callback");
    }
    static void validateRetention(Method method) {
        if (method.getReturnType()!=long.class || method.getParameterCount()!=0
                || Modifier.isAbstract(method.getModifiers()) || Modifier.isNative(method.getModifiers()))
            throw new IllegalArgumentException("Not a retention duration getter");
    }
    static final class RetentionTarget {
        final Method method;
        final boolean duration;
        RetentionTarget(Method method, boolean duration) {
            if (duration) validateRetention(method);
            if (Modifier.isAbstract(method.getModifiers()) || Modifier.isNative(method.getModifiers()))
                throw new IllegalArgumentException("No runtime implementation");
            this.method=method; this.duration=duration;
            method.setAccessible(true);
        }
    }
    static RetentionTarget retention(Context context, ClassLoader loader) throws ReflectiveOperationException {
        SharedPreferences cache = context.getSharedPreferences("pixelboard_runtime_targets", 0);
        String fingerprint = "retention-v2-scoped|"+GboardResolver.fingerprint(context);
        if (fingerprint.equals(cache.getString("key", ""))) {
            if (cache.getBoolean("missing", false)) throw new IllegalArgumentException("Cached missing/ambiguous target");
            try {
                String args=cache.getString("args", "");
                Method method = new TargetMethod(cache.getString("owner", ""), cache.getString("name", ""),
                        cache.getString("return", ""), args.isEmpty()?new String[0]:args.split(",")).resolve(loader);
                return new RetentionTarget(method, cache.getBoolean("duration", false));
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                cache.edit().clear().apply();
            }
        }
        List<RetentionTarget> durations = new ArrayList<>(), readers = new ArrayList<>();
        try (HostDex dex = new HostDex(context)) {
            for (MethodData data : dex.findMethod(FindMethod.create().matcher(MethodMatcher.create()
                    .usingStrings(ClipboardRetention.NATIVE_KEY)))) {
                boolean convertsHours = false, clock = false, readsPreference = false;
                for (MethodData call : data.getInvokes()) {
                    if (call.getClassName().equals("java.util.concurrent.TimeUnit")
                            && call.getMethodName().equals("toMillis")
                            && call.getParamTypeNames().equals(Collections.singletonList("long"))) convertsHours = true;
                    if (call.getClassName().equals("java.lang.System")
                            && call.getMethodName().equals("currentTimeMillis")) clock = true;
                    if (call.getMethodName().equals("getString")
                            && call.getParamTypeNames().equals(Arrays.asList("java.lang.String", "java.lang.String")))
                        readsPreference = true;
                }
                if (!convertsHours || !readsPreference || data.getMethodName().startsWith("<")) continue;
                Method method = data.getMethodInstance(loader);
                boolean duration = !clock && method.getReturnType()==long.class && method.getParameterCount()==0;
                RetentionTarget target = new RetentionTarget(method, duration);
                if (duration) durations.add(target); else readers.add(target);
            }
        }
        List<RetentionTarget> matches = durations.isEmpty()?readers:durations;
        if (matches.size()!=1) {
            cache.edit().clear().putString("key", fingerprint).putBoolean("missing", true).apply();
            throw new IllegalArgumentException("Retention target count="+matches.size());
        }
        RetentionTarget target = matches.get(0);
        Method method=target.method;
        List<String> args=new ArrayList<>();
        for (Class<?> type:method.getParameterTypes()) args.add(type.getName());
        cache.edit().clear().putString("key", fingerprint)
                .putString("owner", method.getDeclaringClass().getName()).putString("name", method.getName())
                .putString("return", method.getReturnType().getName()).putString("args", String.join(",", args))
                .putBoolean("duration", target.duration).apply();
        return target;
    }
}
