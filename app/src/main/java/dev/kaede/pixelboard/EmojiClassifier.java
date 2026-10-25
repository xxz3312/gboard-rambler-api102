package dev.kaede.pixelboard;

import android.icu.lang.UCharacter;
import android.icu.lang.UProperty;

/** Unicode emoji presentation, rather than all surrogates or the entire symbols block. */
public final class EmojiClassifier {
    private EmojiClassifier() {}
    public static boolean isEmojiOnly(CharSequence text) {
        if (!containsEmoji(text)) return false;
        for (int i=0;i<text.length();) {
            int cp = Character.codePointAt(text, i);
            if (!(UCharacter.hasBinaryProperty(cp, UProperty.EMOJI) || cp == 0xFE0F
                    || cp == 0x200D || cp == 0x20E3 || cp >= 0xE0020 && cp <= 0xE007F
                    || Character.isWhitespace(cp))) return false;
            // Digits and #/* are emoji bases only when followed by a keycap sequence.
            if (cp == '#' || cp == '*' || cp >= '0' && cp <= '9') {
                int next = i+1;
                if (next < text.length() && text.charAt(next) == 0xFE0F) next++;
                if (next >= text.length() || text.charAt(next) != 0x20E3) return false;
            }
            i += Character.charCount(cp);
        }
        return true;
    }
    public static boolean containsEmoji(CharSequence text) {
        if (text == null) return false;
        for (int i=0;i<text.length();) {
            int cp = Character.codePointAt(text, i);
            int next = i + Character.charCount(cp);
            if (UCharacter.hasBinaryProperty(cp, UProperty.EMOJI_PRESENTATION)) return true;
            if (next < text.length() && Character.codePointAt(text, next) == 0xFE0F
                    && UCharacter.hasBinaryProperty(cp, UProperty.EMOJI)) return true;
            if ((cp == '#' || cp == '*' || cp >= '0' && cp <= '9') && next < text.length()) {
                int p = next;
                if (Character.codePointAt(text, p) == 0xFE0F) p++;
                if (p < text.length() && Character.codePointAt(text, p) == 0x20E3) return true;
            }
            i = next;
        }
        return false;
    }
}
