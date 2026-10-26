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
        if (method.getReturnType()!=long.class || !(method.getParameterCount()==0 || Arrays.equals(method.getParameterTypes(),new Class<?>[]{Context.class}))
                || Modifier.isAbstract(method.getModifiers()) || Modifier.isNative(method.getModifiers()))
            throw new IllegalArgumentException("Not a retention duration getter");
    }
    static final class RetentionTarget {
        final Method method;
        final boolean duration;
        final String strategy;
        RetentionTarget(Method method, boolean duration) { this(method,duration,duration?"legacy-ms":"legacy-reader"); }
        RetentionTarget(Method method, boolean duration, String strategy) {
            if (duration) validateRetention(method);
            if (Modifier.isAbstract(method.getModifiers()) || Modifier.isNative(method.getModifiers()))
                throw new IllegalArgumentException("No runtime implementation");
            this.method=method; this.duration=duration; this.strategy=strategy;
            method.setAccessible(true);
        }
    }
    static RetentionTarget retention(Context context, ClassLoader loader) throws ReflectiveOperationException {
        SharedPreferences cache = context.getSharedPreferences("pixelboard_runtime_targets", 0);
        String fingerprint = "retention-v3-native-duration|"+GboardResolver.fingerprint(context);
        if (fingerprint.equals(cache.getString("key", ""))) {
            if (cache.getBoolean("missing", false)) throw new IllegalArgumentException("Cached missing/ambiguous target");
            try {
                String args=cache.getString("args", "");
                Method method = new TargetMethod(cache.getString("owner", ""), cache.getString("name", ""),
                        cache.getString("return", ""), args.isEmpty()?new String[0]:args.split(",")).resolve(loader);
                String strategy=cache.getString("strategy","legacy-reader");
                if ("native-duration".equals(strategy)) validateNativeRetention(method);
                return new RetentionTarget(method, cache.getBoolean("duration", false),strategy);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                cache.edit().clear().apply();
            }
        }
        List<RetentionTarget> readers = new ArrayList<>();
        try (HostDex dex = new HostDex(context)) {
            List<RetentionTarget> nativeGetters=new ArrayList<>();
            for (MethodData data:dex.findMethod(FindMethod.create().matcher(MethodMatcher.create()
                    .returnType("long").paramTypes("android.content.Context")
                    .usingStrings(RetentionSemantics.OWNER,RetentionSemantics.METHOD,RetentionSemantics.FAILURE)))) {
                List<String> calls=new ArrayList<>();
                for (MethodData call:data.getInvokes()) calls.add(call.getClassName()+"#"+call.getMethodName()
                        +call.getParamTypeNames()+":"+call.getReturnTypeName());
                if (!RetentionSemantics.matches(data.getUsingStrings(),calls)) continue;
                Method method=data.getMethodInstance(loader);
                validateNativeRetention(method);
                nativeGetters.add(new RetentionTarget(method,true,"native-duration"));
            }
            if (nativeGetters.size()>1) throw new IllegalArgumentException("Native retention target count="+nativeGetters.size());
            if (nativeGetters.size()==1) {
                RetentionTarget target=nativeGetters.get(0);
                saveRetention(cache,fingerprint,target);
                return target;
            }
            for (MethodData data : dex.findMethod(FindMethod.create().matcher(MethodMatcher.create()
                    .usingStrings(ClipboardRetention.NATIVE_KEY)))) {
                boolean convertsHours = false, readsPreference = false;
                for (MethodData call : data.getInvokes()) {
                    if (call.getClassName().equals("java.util.concurrent.TimeUnit")
                            && call.getMethodName().equals("toMillis")
                            && call.getParamTypeNames().equals(Collections.singletonList("long"))) convertsHours = true;
                    if (call.getMethodName().equals("getString")
                            && call.getParamTypeNames().equals(Arrays.asList("java.lang.String", "java.lang.String")))
                        readsPreference = true;
                }
                if (!convertsHours || !readsPreference || data.getMethodName().startsWith("<")) continue;
                Method method = data.getMethodInstance(loader);
                // The old preference path only scopes reads; its long return may be an epoch cutoff.
                readers.add(new RetentionTarget(method, false));
            }
        }
        List<RetentionTarget> matches = readers;
        if (matches.size()!=1) {
            cache.edit().clear().putString("key", fingerprint).putBoolean("missing", true).apply();
            throw new IllegalArgumentException("Retention target count="+matches.size());
        }
        RetentionTarget target = matches.get(0);
        saveRetention(cache,fingerprint,target);
        return target;
    }
    static void validateNativeRetention(Method method) {
        validateRetention(method);
        if (!Modifier.isStatic(method.getModifiers())
                || !Arrays.equals(method.getParameterTypes(),new Class<?>[]{Context.class}))
            throw new IllegalArgumentException("Invalid native retention getter contract");
    }
    private static void saveRetention(SharedPreferences cache,String fingerprint,RetentionTarget target) {
        Method method=target.method;
        List<String> args=new ArrayList<>();
        for (Class<?> type:method.getParameterTypes()) args.add(type.getName());
        cache.edit().clear().putString("key", fingerprint)
                .putString("owner", method.getDeclaringClass().getName()).putString("name", method.getName())
                .putString("return", method.getReturnType().getName()).putString("args", String.join(",", args))
                .putString("strategy",target.strategy).putBoolean("duration", target.duration).apply();
    }
}
