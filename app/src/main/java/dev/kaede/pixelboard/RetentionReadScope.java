package dev.kaede.pixelboard;

/** Native preference overrides are confined to a semantically verified runtime calculation. */
final class RetentionReadScope implements AutoCloseable {
    private static final ThreadLocal<Integer> DEPTH = new ThreadLocal<>();
    RetentionReadScope() { DEPTH.set(depth()+1); }
    private static int depth() { Integer value=DEPTH.get(); return value==null ? 0 : value; }
    static boolean active() { return depth()>0; }
    static Object override(Object key, Object original, String selection) {
        String valid=ClipboardRetention.sanitize(selection);
        return active() && ClipboardRetention.NATIVE_KEY.equals(key) && !"stock".equals(valid) ? valid : original;
    }
    @Override public void close() {
        int value=depth()-1;
        if (value<=0) DEPTH.remove(); else DEPTH.set(value);
    }
}
