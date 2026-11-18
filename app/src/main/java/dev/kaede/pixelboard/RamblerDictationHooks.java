package dev.kaede.pixelboard;

import android.content.Context;
import io.github.libxposed.api.XposedModule;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;

/** Exact upstream callback families plus a DEX class anchor; unknown builds are skipped. */
final class RamblerDictationHooks {
    private static final String EXTENSION =
            "com/google/android/apps/inputmethod/libs/agenticdictation/extension/AgenticDictationExtension";
    private static final String[][] CALLBACKS = {
        {"fhv", "aaru", "sgm"}, {"fde", "aajc", "rzc"}, {"ffh", "aaks", "sao"}
    };
    static void install(XposedModule module, Context context, ClassLoader loader) {
        try {
            List<Method> matches = new ArrayList<>();
            try (HostDex dex = new HostDex(context)) {
                for (String[] family : CALLBACKS) {
                    try {
                        Method method = new TargetMethod(family[0], "j", "void",
                                family[1], family[2], "java.lang.String").resolve(loader);
                        if (Modifier.isStatic(method.getModifiers())) continue;
                        Field spans = method.getParameterTypes()[0].getDeclaredField("b");
                        if (!List.class.isAssignableFrom(spans.getType())) continue;
                        if (dex.findMethod(FindMethod.create().matcher(MethodMatcher.create()
                                .declaredClass(family[0]).usingStrings(EXTENSION, "onCreateExtension"))).isEmpty()) continue;
                        matches.add(method);
                    } catch (ReflectiveOperationException | IllegalArgumentException | LinkageError ignored) {}
                }
            }
            if (matches.size() != 1) throw new IllegalArgumentException("callback count=" + matches.size());
            module.hook(matches.get(0)).intercept(chain -> {
                if (FeatureConfig.rambler() && RamblerSelection.mayExpose(context)) {
                    Object proto = chain.getArgs().get(0);
                    // Never inspect or log the user's transcript; validate all field types before mutation.
                    if (validProto(proto))
                        RamblerDictationRuntime.filterVoiceText(chain.getThisObject(), proto);
                }
                return chain.proceed();
            });
            CloudRuntimeStatus.send(context, "DICTATION_REPAIR", "installed:upstream-1.3.5");
        } catch (RuntimeException | LinkageError error) {
            CloudRuntimeStatus.send(context, "DICTATION_REPAIR", "skipped:" + error.getClass().getSimpleName());
        }
    }
    static boolean validProto(Object proto) {
        if (proto == null) return false;
        try {
            Field spansField = field(proto.getClass(), "b");
            Object value = spansField.get(proto);
            if (!(value instanceof List)) return false;
            for (Object span : (List<?>) value) {
                if (span == null || field(span.getClass(), "c").getType() != String.class) return false;
                Object action = field(span.getClass(), "j").get(span);
                if (action == null) continue;
                if (field(action.getClass(), "b").getType() != int.class
                        || field(action.getClass(), "c").getType() != int.class
                        || field(action.getClass(), "d").getType() != String.class) return false;
                Object diff = field(action.getClass(), "e").get(action);
                if (diff != null) for (String name : new String[]{"c", "d", "e"})
                    if (field(diff.getClass(), name).getType() != String.class) return false;
            }
            return true;
        } catch (ReflectiveOperationException | RuntimeException error) { return false; }
    }
    private static Field field(Class<?> owner, String name) throws NoSuchFieldException {
        Field field = owner.getDeclaredField(name);
        if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers()))
            throw new IllegalArgumentException("Immutable or static proto field");
        field.setAccessible(true);
        return field;
    }
}
