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
    static Method retention(Context context, ClassLoader loader) throws ReflectiveOperationException {
        SharedPreferences cache = context.getSharedPreferences("pixelboard_runtime_targets", 0);
        String fingerprint = "retention-v1|"+GboardResolver.fingerprint(context);
        if (fingerprint.equals(cache.getString("key", ""))) {
            try {
                Method method = Class.forName(cache.getString("owner", ""), false, loader)
                        .getDeclaredMethod(cache.getString("name", ""));
                validateRetention(method);
                method.setAccessible(true);
                return method;
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                cache.edit().clear().apply();
            }
        }
        List<Method> matches = new ArrayList<>();
        try (HostDex dex = new HostDex(context)) {
            for (MethodData data : dex.findMethod(FindMethod.create().matcher(MethodMatcher.create()
                    .returnType("long").paramCount(0).usingStrings(ClipboardRetention.NATIVE_KEY)))) {
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
                // Never replace an epoch cutoff or a generic long-returning cleanup method.
                if (!convertsHours || clock || !readsPreference) continue;
                Method method = data.getMethodInstance(loader);
                validateRetention(method);
                matches.add(method);
            }
        }
        Method method = GboardResolver.unique(matches);
        method.setAccessible(true);
        cache.edit().clear().putString("key", fingerprint)
                .putString("owner", method.getDeclaringClass().getName())
                .putString("name", method.getName()).apply();
        return method;
    }
}
