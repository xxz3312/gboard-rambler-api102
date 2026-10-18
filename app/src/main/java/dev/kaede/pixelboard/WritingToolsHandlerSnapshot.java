package dev.kaede.pixelboard;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.Future;

/** Exact 18.4 metadata snapshot. Never calls a model getter, starts work or reads text. */
public final class WritingToolsHandlerSnapshot {
    private WritingToolsHandlerSnapshot() {}
    public static String read(ClassLoader loader) {
        try {
            Class<?> managerType = Class.forName("ojm", false, loader);
            Object manager = field(managerType, "a", "ojm").get(null);
            Method kind = new TargetMethod("qrr", "b", "int").resolve(loader);
            StringBuilder line = new StringBuilder("kind=").append(kind.invoke(null));
            if (manager == null) return line.append(" manager=none").toString();
            Object handler = field(managerType, "b", "ojx").get(manager);
            if (handler == null) return line.append(" handler=none").toString();
            line.append(" handler=").append(handler.getClass().getName());
            Class<?> hybridType = Class.forName("hfz", false, loader);
            if (hybridType.isInstance(handler)) {
                line.append(" status=").append(enumName(field(hybridType, "g", "oiz").get(handler)));
                line.append(" statusCheck=").append(futureState(field(hybridType, "h", "xin").get(handler)));
                line.append(" localPrepare=").append(futureState(field(hybridType, "i", "xin").get(handler)));
            }
            try {
            Object wrapper = field(managerType, "c", "ojl").get(manager);
            if (wrapper != null) {
                Class<?> wrapperType = Class.forName("ojl", false, loader);
                Object requestHandler = field(wrapperType, "a", "oka").get(wrapper);
                Class<?> requestType = Class.forName("hgm", false, loader);
                if (requestType.isInstance(requestHandler)) {
                    line.append(" privateStatus=").append(enumName(field(requestType, "h", "oiz").get(requestHandler)));
                    line.append(" localStatus=").append(enumName(field(requestType, "i", "oiz").get(requestHandler)));
                    line.append(" joinedCheck=").append(futureState(field(requestType, "n", "xin").get(requestHandler)));
                }
            }
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                line.append(" requestMetadata=unavailable");
            }
            return line.toString();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return "snapshot=unavailable_" + e.getClass().getSimpleName();
        }
    }
    private static Field field(Class<?> owner, String name, String type) throws ReflectiveOperationException {
        Field f = owner.getDeclaredField(name);
        if (!type.equals(f.getType().getName())) throw new NoSuchFieldException(owner.getName() + "." + name);
        f.setAccessible(true);
        return f;
    }
    private static String enumName(Object value) {
        return value instanceof Enum<?> ? ((Enum<?>) value).name() : "<unknown>";
    }
    private static String futureState(Object value) {
        if (!(value instanceof Future<?>)) return "none";
        Future<?> future = (Future<?>) value;
        return future.isCancelled() ? "cancelled" : future.isDone() ? "done" : "pending";
    }
}
