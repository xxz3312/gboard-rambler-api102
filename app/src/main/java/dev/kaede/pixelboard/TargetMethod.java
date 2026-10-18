package dev.kaede.pixelboard;

import java.lang.reflect.Method;
import java.util.Arrays;

/** Exact descriptor matching; prevents an obfuscation drift from choosing a random method. */
public final class TargetMethod {
    public final String owner, name, returnType;
    public final String[] arguments;
    public TargetMethod(String owner, String name, String returnType, String... arguments) {
        this.owner = owner; this.name = name; this.returnType = returnType;
        this.arguments = arguments.clone();
    }
    public Method resolve(ClassLoader loader) throws ReflectiveOperationException {
        Class<?> cls = Class.forName(owner, false, loader);
        Class<?>[] types = new Class<?>[arguments.length];
        for (int i = 0; i < arguments.length; i++) types[i] = type(loader, arguments[i]);
        Method m = cls.getDeclaredMethod(name, types);
        if (m.getReturnType() != type(loader, returnType))
            throw new NoSuchMethodException("Return mismatch: " + this);
        m.setAccessible(true);
        return m;
    }
    private static Class<?> type(ClassLoader loader, String name) throws ClassNotFoundException {
        switch (name) {
            case "void": return void.class;
            case "boolean": return boolean.class;
            case "int": return int.class;
            case "long": return long.class;
            default: return Class.forName(name, false, loader);
        }
    }
    @Override public String toString() {
        return owner + "#" + name + Arrays.toString(arguments) + ":" + returnType;
    }
}
