package dev.kaede.pixelboard;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;

/** Exact PCS 149829 schema. Only allowlisted labels and byte lengths leave this reader. */
final class PcsManifestMetadata {
    static final String CLIENT = "com.google.android.aicore:18103149225492435673";
    private static final Set<String> GROUPS = Set.of("all", "beta", "alpha", "third_party_eap", "third_party_experimental");
    private static final Set<String> TIERS = Set.of("", "Ultra Low", "Low", "Mid", "High", "Ultra");
    static final class Binding {
        final Field config, client, labels, release, appVersion, labelName, labelValue;
        final Field proof, proofMode, token, extraProof, publicKey, keyBytes, options, option;
        final Method byteSize;
        Binding(Class<?> request, Class<?> cfg, Class<?> label, Class<?> releaseType,
                Class<?> proofType, Class<?> bytes, Class<?> keyType, Class<?> optionType)
                throws ReflectiveOperationException {
            config = field(request, "c", cfg); client = field(cfg, "c", String.class);
            labels = cfg.getDeclaredField("d");
            if (!List.class.isAssignableFrom(labels.getType()) || Modifier.isStatic(labels.getModifiers()))
                throw new NoSuchFieldException("Labels schema mismatch");
            labels.setAccessible(true);
            release = field(cfg, "e", releaseType); appVersion = field(releaseType, "b", long.class);
            labelName = field(label, "b", String.class); labelValue = field(label, "c", String.class);
            proof = field(request, "e", proofType); proofMode = field(proofType, "c", int.class);
            token = field(proofType, "b", bytes); extraProof = field(proofType, "d", bytes);
            publicKey = field(request, "d", keyType); keyBytes = field(keyType, "b", bytes);
            options = field(request, "f", optionType); option = field(optionType, "b", boolean.class);
            byteSize = bytes.getDeclaredMethod("d");
            if (byteSize.getReturnType() != int.class || Modifier.isStatic(byteSize.getModifiers()))
                throw new NoSuchMethodException("Byte size schema mismatch");
            byteSize.setAccessible(true);
        }
        String describe(Object request) throws ReflectiveOperationException {
            Object cfg = config.get(request);
            if (cfg == null || !CLIENT.equals(client.get(cfg))) return null;
            Object raw = labels.get(cfg);
            if (!(raw instanceof List)) throw new IllegalArgumentException("Missing labels");
            List<?> values = (List<?>) raw;
            String build = "absent", group = "absent", tier = "absent", variant = "absent";
            int unknown = 0;
            for (int i = 0; i < Math.min(values.size(), 32); i++) {
                Object v = values.get(i);
                String k = (String) labelName.get(v), text = (String) labelValue.get(v);
                if ("build_id".equals(k)) build = text != null && text.matches("[0-9]{1,19}") ? text : "redacted";
                else if ("client_group".equals(k)) group = GROUPS.contains(text == null ? "?" : text) ? text : "redacted";
                else if ("device_tier".equals(k)) tier = TIERS.contains(text == null ? "?" : text) ? (text.isEmpty() ? "empty" : text.replace(' ', '_')) : "redacted";
                else if ("variant".equals(k)) variant = safeVariant(text);
                else unknown++;
            }
            Object rel = release.get(cfg), att = proof.get(request), key = publicKey.get(request), opt = options.get(request);
            return "client=AI_CORE_CLIENT_37 build=" + build + " tier=" + tier + " group=" + group
                    + " variant=" + variant + " apk_version=" + (rel == null ? -1 : appVersion.getLong(rel))
                    + " labels=" + values.size() + " unknown_labels=" + unknown
                    + " att_mode=" + (att == null ? -1 : proofMode.getInt(att))
                    + " token_n=" + (att == null ? -1 : size(token.get(att)))
                    + " extra_n=" + (att == null ? -1 : size(extraProof.get(att)))
                    + " key_n=" + (key == null ? -1 : size(keyBytes.get(key)))
                    + " option=" + (opt != null && option.getBoolean(opt));
        }
        private int size(Object bytes) throws ReflectiveOperationException {
            return bytes == null ? 0 : (Integer) byteSize.invoke(bytes);
        }
    }
    private static String safeVariant(String text) {
        if (text != null && (text.matches("VARIANT_[0-9]{1,2}") || Set.of("VARIANT_UNSPECIFIED", "VARIANT_OEM", "VARIANT_PIXEL", "VARIANT_SAMSUNG_QC", "VARIANT_SAMSUNG_SLSI").contains(text))) return text;
        return "redacted";
    }
    private static Field field(Class<?> owner, String name, Class<?> type) throws ReflectiveOperationException {
        Field f = owner.getDeclaredField(name);
        if (f.getType() != type || Modifier.isStatic(f.getModifiers())) throw new NoSuchFieldException("PCS schema mismatch");
        f.setAccessible(true); return f;
    }
}
