package dev.kaede.pixelboard;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Set;

/** Does not emit exception messages, trailers, tokens, keys or response bodies. */
final class PcsRpcErrorMetadata {
    private static final Set<String> CODES = Set.of("OK", "CANCELLED", "UNKNOWN", "INVALID_ARGUMENT", "DEADLINE_EXCEEDED", "NOT_FOUND", "ALREADY_EXISTS", "PERMISSION_DENIED", "RESOURCE_EXHAUSTED", "FAILED_PRECONDITION", "ABORTED", "OUT_OF_RANGE", "UNIMPLEMENTED", "INTERNAL", "UNAVAILABLE", "DATA_LOSS", "UNAUTHENTICATED");
    static final class Binding {
        final Class<?> errorType, statusType;
        final Field status, trailers, code, description;
        Binding(Class<?> error, Class<?> statusClass, Class<?> enumClass, Class<?> metadata) throws ReflectiveOperationException {
            errorType = error; statusType = statusClass;
            if (!Throwable.class.isAssignableFrom(error) || !enumClass.isEnum()) throw new NoSuchFieldException("RPC error schema mismatch");
            status = field(error, "a", statusClass); trailers = field(error, "b", metadata);
            code = field(statusClass, "r", enumClass); description = field(statusClass, "s", String.class);
        }
        String describe(Throwable error) throws ReflectiveOperationException {
            Throwable t = error;
            for (int i = 0; t != null && i < 8; i++, t = t.getCause()) {
                Object s = null; boolean hasTrailers = false;
                if (errorType.isInstance(t)) {
                    s = status.get(t); hasTrailers = trailers.get(t) != null;
                } else if ("io.grpc.StatusException".equals(t.getClass().getName())) {
                    s = t.getClass().getMethod("getStatus").invoke(t);
                    hasTrailers = t.getClass().getMethod("getTrailers").invoke(t) != null;
                }
                if (statusType.isInstance(s)) {
                    Object c = code.get(s);
                    String name = c instanceof Enum ? ((Enum<?>) c).name() : "UNKNOWN";
                    if (!CODES.contains(name)) name = "UNKNOWN";
                    String d = (String) description.get(s);
                    String category = "Request contains an invalid argument.".equals(d) ? "GENERIC_INVALID_ARGUMENT"
                            : d == null || d.isEmpty() ? "EMPTY" : "OTHER_REDACTED";
                    return "rpc=" + name + " description=" + category + " trailersPresent=" + hasTrailers;
                }
            }
            return "rpc=UNKNOWN cause=" + (error == null ? "none" : safeClass(error.getClass()));
        }
    }
    private static String safeClass(Class<?> type) {
        String n = type.getSimpleName(); return n.matches("[A-Za-z0-9_$]{1,80}") ? n : "redacted";
    }
    private static Field field(Class<?> owner, String name, Class<?> type) throws ReflectiveOperationException {
        Field f = owner.getDeclaredField(name);
        if (f.getType() != type || Modifier.isStatic(f.getModifiers())) throw new NoSuchFieldException("RPC schema mismatch");
        f.setAccessible(true); return f;
    }
}
