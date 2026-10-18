package dev.kaede.pixelboard;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** Conservative mapping derived from PixelBoard's GboardFlagFamilyComposer (GPL-3.0). */
public final class RamblerFlagBindings {
    private RamblerFlagBindings() {}
    public static final class Binding {
        public final String profile, owner;
        public final Method getter;
        private final Field nameField;
        private Binding(String profile, String owner, Method getter, Field field) {
            this.profile = profile; this.owner = owner;
            this.getter = getter; this.nameField = field;
        }
        public String name(Object receiver) throws IllegalAccessException {
            Object value = nameField.get(receiver);
            return value instanceof String ? (String)value : null;
        }
    }
    private static final String[][] CANDIDATES = {
        {"18.4.1", "odx", "g", "a"},
        {"18.3.1", "obw", "g", "a"},
        {"18.3.1", "acps", "g", "a"}
        // 18.0.3 nxw name-field binding remains unverified: don't assume nxw.a.
    };
    /**
     * Independent typed getter resolution for Writing Tools. Fail closed if more
     * than one symbol family matches; never guess by obfuscated name alone.
     */
    public static Binding resolveUnique(ClassLoader loader) {
        Binding found = null;
        for (String[] candidate : CANDIDATES) {
            Binding current = resolveSingle(loader, candidate);
            if (current == null) continue;
            if (found != null) return null;
            found = current;
        }
        return found;
    }
    private static Binding resolveSingle(ClassLoader loader, String[] c) {
        try {
            Class<?> owner = Class.forName(c[1], false, loader);
            Method getter = owner.getDeclaredMethod(c[2]);
            Field name = owner.getDeclaredField(c[3]);
            if (Modifier.isStatic(getter.getModifiers()) ||
                Modifier.isStatic(name.getModifiers()) ||
                getter.getReturnType() != Object.class ||
                name.getType() != String.class) return null;
            getter.setAccessible(true);
            name.setAccessible(true);
            return new Binding(c[0], c[1], getter, name);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return null;
        }
    }
    public static Binding resolve(ClassLoader loader, String profile) {
        Binding found = null;
        for (String[] c : CANDIDATES) {
            if (!profile.equals(c[0])) continue;
            try {
                Class<?> owner = Class.forName(c[1], false, loader);
                Method getter = owner.getDeclaredMethod(c[2]);
                Field name = owner.getDeclaredField(c[3]);
                if (Modifier.isStatic(getter.getModifiers()) ||
                    Modifier.isStatic(name.getModifiers()) ||
                    getter.getReturnType() != Object.class ||
                    name.getType() != String.class) continue;
                getter.setAccessible(true);
                name.setAccessible(true);
                if (found != null) return null;
                found = new Binding(c[0], c[1], getter, name);
            } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {}
        }
        return found;
    }
}
