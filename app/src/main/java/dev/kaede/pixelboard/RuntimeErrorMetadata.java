package dev.kaede.pixelboard;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Bounded, message-free cause metadata; does not expose submitted text or credentials. */
public final class RuntimeErrorMetadata {
    private RuntimeErrorMetadata() {}
    public static String describe(Throwable failure) {
        StringBuilder out = new StringBuilder("causes=");
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable current = failure;
        int count = 0;
        while (current != null && count < 4 && seen.add(current)) {
            if (count > 0) out.append(">");
            out.append(current.getClass().getName());
            count++;
            current = current.getCause();
        }
        if (current != null) out.append(">...");
        return out.toString();
    }
}
