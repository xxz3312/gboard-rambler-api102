package dev.kaede.pixelboard;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 * Adapted from Akshay Kadam / PixelBoard GboardRamblerDictationRuntime (GPL-3.0),
 * revision 65c21b61c4b41542417522530152895a828ab1c6.
 * Restore only COMPOSE responses; preserve rewrite/edit/history voice commands (#90).
 */
public final class RamblerDictationFilter {
    private RamblerDictationFilter() {}
    static Field field(Class<?> type, String name, Class<?> expected) throws ReflectiveOperationException {
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(name);
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())
                        || !expected.isAssignableFrom(field.getType())) throw new NoSuchFieldException(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException error) {
                if (current.getSuperclass() == Object.class) throw error;
            }
        }
        throw new NoSuchFieldException(name);
    }
    public static void filter(Object voiceText) {
        if (voiceText == null) return;
        try {
            Object value = field(voiceText.getClass(), "b", List.class).get(voiceText);
            if (!(value instanceof List<?>)) return;
            for (Object span : (List<?>) value) {
                if (span == null) continue;
                try {
                    Field actionField = span.getClass().getDeclaredField("j");
                    if (Modifier.isStatic(actionField.getModifiers())) continue;
                    actionField.setAccessible(true);
                    Object action = actionField.get(span);
                    if (action == null) continue;
                    Field intent = field(action.getClass(), "c", int.class);
                    if (intent.getInt(action) != 4) continue;
                    String transcript = (String) field(action.getClass(), "d", String.class).get(action);
                    if (transcript == null || transcript.isEmpty()) continue;
                    Field text = field(span.getClass(), "c", String.class);
                    text.set(span, transcript);
                    intent.setInt(action, 0);
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    // Unexpected schema: leave this span untouched and continue other spans.
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) { }
    }
}
