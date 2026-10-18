package dev.kaede.pixelboard;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Stock obfuscated getter and name field from upstream GboardFlagFamilyComposer.
 * A mapping matches only if the exact method and String name field both exist.
 */
public final class RamblerFlagBindings {
    private RamblerFlagBindings() {}
    public static final class Binding {
        public final String profile, owner, methodName, fieldName;
        public final Method getter;
        public final Field nameField;
        private Binding(String profile, String owner, String methodName, String fieldName,
                        Method getter, Field nameField) {
            this.profile = profile; this.owner = owner; this.methodName = methodName;
            this.fieldName = fieldName; this.getter = getter; this.nameField = nameField;
        }
        public String name(Object receiver) throws IllegalAccessException {
            Object value = nameField.get(receiver);
            return value instanceof String ? (String)value : null;
        }
    }
    private static final String[][] CANDIDATES = {
        {"18.4.1", "odx", "g", "a"},
        {"18.3.1-release", "obw", "g", "a"},
        {"18.3.1", "acps", "g", "a"},
        {"18.0.3", "nxw", "g", "a"}
    };
    public static Binding resolve(ClassLoader loader, String selectedProfile) {
        Binding found = null;
        for (String[] candidate : CANDIDATES) {
            if (!candidate[0].startsWith(selectedProfile)) continue;
            try {
                Class<?> owner = Class.forName(candidate[1], false, loader);
                Method getter = owner.getDeclaredMethod(candidate[2]);
                if (getter.getReturnType() != Object.class) continue;
                Field field = owner.getDeclaredField(candidate[3]);
                if (field.getType() != String.class) continue;
                getter.setAccessible(true);
                field.setAccessible(true);
                if (found != null) return null; // ambiguity: fail closed
                found = new Binding(candidate[0], candidate[1], candidate[2],
                        candidate[3], getter, field);
            } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
                // Missing or drifted symbol; do not guess a replacement.
            }
        }
        return found;
    }
}
