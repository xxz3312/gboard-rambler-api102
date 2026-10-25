package dev.kaede.pixelboard;

import java.util.Set;

/** Flag policy ported from Akshayykadam/PixelBoard 1.3.2 (GPL-3.0). */
public final class KeyboardFeaturePolicy {
    private KeyboardFeaturePolicy() {}
    private static final Set<String> OCR = Set.of("enable_ocr", "config_ocr",
            "enable_camera_permission_overlay");
    private static final Set<String> SCREENSHOT = Set.of("enable_clipboard_image_suggestion",
            "enable_screenshot_notification_flow", "enable_screenshot_chip",
            "enable_clipboard_recent_screenshot", "clipboard_screenshot_enabled_at_least_once",
            "enable_screenshot_in_clipboard");
    private static final Set<String> ROUNDED = Set.of("belka_rounded_keyboard", "belka_ui",
            "belka_background", "more_pill_keys", "enable_rounded_key_by_default", "enable_rounded_keys");
    public static boolean targeted(String name) {
        return name != null && (OCR.contains(name) || SCREENSHOT.contains(name)
                || ROUNDED.contains(name) || "enabled_ocr_language_tags".equals(name));
    }
    static Object overrideValue(String name, Object original, boolean ocr, boolean screenshot, boolean rounded) {
        if (name == null) return original;
        if (ocr && "enabled_ocr_language_tags".equals(name) && original instanceof String) return "*";
        if (!(original instanceof Boolean)) return original;
        if (ocr && OCR.contains(name) || screenshot && SCREENSHOT.contains(name)
                || rounded && ROUNDED.contains(name)) return Boolean.TRUE;
        return original;
    }
    public static Object transform(String name, Object original) {
        return overrideValue(name, original, FeatureConfig.scanText(),
                FeatureConfig.screenshot(), FeatureConfig.roundedKeys());
    }
}
