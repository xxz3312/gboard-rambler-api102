package dev.kaede.pixelboard;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Runtime-derived field slots only; never uses guessed offsets or absolute addresses. */
final class StaticStringFieldWriter {
    private final Object unsafe;
    private final Method putReference;
    private final Map<String, Slot> slots = new LinkedHashMap<>();
    final String mechanism;
    private static final class Slot {
        final Object base;
        final long offset;
        Slot(Object base, long offset) { this.base = base; this.offset = offset; }
    }
    static StaticStringFieldWriter create(Class<?> owner, String[] allowed) throws Exception {
        Exception failure = null;
        for (String className : new String[] {"jdk.internal.misc.Unsafe", "sun.misc.Unsafe"}) {
            try { return new StaticStringFieldWriter(owner, allowed, className); }
            catch (Exception e) { failure = e; }
        }
        throw failure == null ? new IllegalStateException("No field writer") : failure;
    }
    private StaticStringFieldWriter(Class<?> owner, String[] allowed, String className) throws Exception {
        Class<?> cls = Class.forName(className);
        Field singleton;
        try { singleton = cls.getDeclaredField("theUnsafe"); }
        catch (NoSuchFieldException e) { singleton = cls.getDeclaredField("THE_ONE"); }
        singleton.setAccessible(true);
        unsafe = singleton.get(null);
        if (unsafe == null) throw new IllegalStateException("Missing singleton");
        Method baseMethod = accessible(cls.getDeclaredMethod("staticFieldBase", Field.class));
        Method offsetMethod = accessible(cls.getDeclaredMethod("staticFieldOffset", Field.class));
        Method read;
        Method write;
        try {
            read = cls.getDeclaredMethod("getReferenceVolatile", Object.class, long.class);
            write = cls.getDeclaredMethod("putReferenceVolatile", Object.class, long.class, Object.class);
        } catch (NoSuchMethodException e) {
            read = cls.getDeclaredMethod("getObjectVolatile", Object.class, long.class);
            write = cls.getDeclaredMethod("putObjectVolatile", Object.class, long.class, Object.class);
        }
        accessible(read);
        putReference = accessible(write);
        mechanism = className;
        // Resolve and verify ALL slots before any write is allowed.
        for (String name : allowed) {
            Field field = owner.getDeclaredField(name);
            if (field.getType() != String.class || !Modifier.isStatic(field.getModifiers())
                    || field.getDeclaringClass() != owner) throw new IllegalArgumentException("Invalid field type");
            field.setAccessible(true);
            Object base = baseMethod.invoke(unsafe, field);
            long offset = ((Number) offsetMethod.invoke(unsafe, field)).longValue();
            if (base == null || offset < 0) throw new IllegalStateException("Invalid slot");
            if (!Objects.equals(field.get(null), read.invoke(unsafe, base, offset)))
                throw new IllegalStateException("Slot readback mismatch");
            slots.put(name, new Slot(base, offset));
        }
    }
    void set(String name, String value) throws Exception {
        Slot slot = slots.get(name);
        if (slot == null) throw new IllegalArgumentException("Field outside allowlist");
        putReference.invoke(unsafe, slot.base, slot.offset, value);
    }
    private static Method accessible(Method method) { method.setAccessible(true); return method; }
}
