package dev.kaede.pixelboard;

/** Module selections in hours; native getter overrides use milliseconds. No host writes. */
public final class ClipboardRetention {
    public static final String KEY = "clipboard_retention";
    public static final String NATIVE_KEY = "clipboard_history_retention";
    public static final String[] VALUES = {"stock", "1", "24", "168", "720", "876000"};
    public static final String[] LABELS = {"遵循 Gboard", "1 小时", "24 小时", "7 天", "30 天", "不自动过期（100 年）"};
    private ClipboardRetention() {}
    public static String sanitize(Object value) {
        for (String valid : VALUES) if (valid.equals(value)) return valid;
        return "stock";
    }
    public static int index(String value) {
        for (int i=0;i<VALUES.length;i++) if (VALUES[i].equals(value)) return i;
        return 0;
    }
    public static long millis(String value, long original) {
        String valid = sanitize(value);
        return "stock".equals(valid) ? original : Long.parseLong(valid) * 3_600_000L;
    }
}
