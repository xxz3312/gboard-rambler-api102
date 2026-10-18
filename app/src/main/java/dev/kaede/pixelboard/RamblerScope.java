package dev.kaede.pixelboard;

/** Reentrant thread-local scope mirroring PixelBoard's selector-scoped architecture. */
public final class RamblerScope {
    private RamblerScope() {}
    private static final ThreadLocal<Integer> VOICE_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Integer> DEFAULT_DEPTH = ThreadLocal.withInitial(() -> 0);

    public static void enterVoice() { VOICE_DEPTH.set(VOICE_DEPTH.get() + 1); }
    public static void exitVoice() { decrement(VOICE_DEPTH); }
    public static void enterDefault() { DEFAULT_DEPTH.set(DEFAULT_DEPTH.get() + 1); }
    public static void exitDefault() { decrement(DEFAULT_DEPTH); }
    public static boolean isVoiceSettings() { return VOICE_DEPTH.get() > 0; }
    public static boolean isDefaultSelection() { return DEFAULT_DEPTH.get() > 0; }
    private static void decrement(ThreadLocal<Integer> depth) {
        int count = depth.get() - 1;
        if (count <= 0) depth.remove(); else depth.set(count);
    }
}
