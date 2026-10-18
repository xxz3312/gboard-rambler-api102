package dev.kaede.pixelboard;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.MethodData;

/** Semantic PCS binding. No obfuscated names or package version allowlist. */
public final class AstreaFlagResolver {
    private static final String MARKER =
            "com/google/android/apps/miphone/astrea/common/config/impl/DeviceFlagManager";
    private static final String REVISION = "device-flag-getter-v1";
    private AstreaFlagResolver() {}

    public static final class Binding {
        public final Method method;
        public final String source;
        Binding(Method method, String source) { this.method = method; this.source = source; }
    }

    public static Binding resolve(Context context, ClassLoader loader) throws Exception {
        if (!"com.google.android.as.oss".equals(context.getPackageName()))
            throw new IllegalArgumentException("Unexpected host");
        String apk = context.getApplicationInfo().sourceDir;
        String fingerprint = fingerprint(apk);
        SharedPreferences cache = context.getSharedPreferences("pixelboard_astrea_binding", 0);
        if (fingerprint.equals(cache.getString("fingerprint", ""))) {
            try {
                Method cached = Class.forName(cache.getString("owner", ""), false, loader)
                        .getDeclaredMethod(cache.getString("method", ""), String.class);
                validate(cached);
                return new Binding(cached, "cache");
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                cache.edit().clear().apply();
            }
        }
        System.loadLibrary("dexkit");
        List<Method> candidates = new ArrayList<>();
        try (DexKitBridge bridge = DexKitBridge.create(apk)) {
            List<ClassData> owners = bridge.findClass(FindClass.create()
                    .matcher(ClassMatcher.create().usingStrings(MARKER)));
            // Shared logging helpers can repeat the marker. Require uniqueness only
            // after the full signature and platform-call constraints are applied.
            for (ClassData owner : owners) for (MethodData candidate : owner.getMethods()) {
                if (!"java.lang.String".equals(candidate.getReturnTypeName())
                        || !candidate.getParamTypeNames().equals(Collections.singletonList("java.lang.String")))
                    continue;
                boolean readsDeviceConfig = false;
                boolean clearsIdentity = false;
                boolean restoresIdentity = false;
                for (MethodData call : candidate.getInvokes()) {
                    if ("android.provider.DeviceConfig".equals(call.getClassName())
                            && "getProperty".equals(call.getMethodName())
                            && "java.lang.String".equals(call.getReturnTypeName())
                            && call.getParamTypeNames().equals(java.util.Arrays.asList(
                                    "java.lang.String", "java.lang.String"))) readsDeviceConfig = true;
                    if ("android.os.Binder".equals(call.getClassName())) {
                        if ("clearCallingIdentity".equals(call.getMethodName())) clearsIdentity = true;
                        if ("restoreCallingIdentity".equals(call.getMethodName())) restoresIdentity = true;
                    }
                }
                if (readsDeviceConfig && clearsIdentity && restoresIdentity) {
                    Method method = candidate.getMethodInstance(loader);
                    validate(method);
                    candidates.add(method);
                }
            }
        }
        Method selected = unique(candidates);
        cache.edit().putString("fingerprint", fingerprint)
                .putString("owner", selected.getDeclaringClass().getName())
                .putString("method", selected.getName()).apply();
        return new Binding(selected, "scan");
    }

    static Method unique(List<Method> candidates) {
        if (candidates.size() != 1)
            throw new IllegalStateException("Getter count=" + candidates.size());
        return candidates.get(0);
    }

    static void validate(Method method) {
        int modifiers = method.getModifiers();
        if (Modifier.isAbstract(modifiers) || Modifier.isNative(modifiers) || Modifier.isStatic(modifiers)
                || method.getReturnType() != String.class
                || !java.util.Arrays.equals(method.getParameterTypes(), new Class<?>[] {String.class}))
            throw new IllegalArgumentException("Getter structure changed");
        method.setAccessible(true);
    }

    private static String fingerprint(String path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(REVISION.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        try (FileInputStream in = new FileInputStream(path)) {
            byte[] buffer = new byte[65536];
            int count;
            while ((count = in.read(buffer)) != -1) digest.update(buffer, 0, count);
        }
        StringBuilder out = new StringBuilder();
        for (byte value : digest.digest()) out.append(String.format(java.util.Locale.ROOT, "%02x", value & 255));
        return out.toString();
    }
}
