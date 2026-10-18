package dev.kaede.pixelboard;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public final class AstreaFlagResolverTest {
    static class Renamed { String a(String key) { return key; } String z(String key) { return key; } }
    static abstract class AbstractGetter { abstract String read(String key); }
    static class Wrong { static String read(String key) { return key; } Boolean typed(String key) { return false; } }
    @Test public void renamedConcreteGettersAreAccepted() throws Exception {
        Method a = Renamed.class.getDeclaredMethod("a", String.class);
        AstreaFlagResolver.validate(a);
        assertSame(a, AstreaFlagResolver.unique(Collections.singletonList(a)));
    }
    @Test public void absentAndAmbiguousMatchesAreRejected() throws Exception {
        assertThrows(IllegalStateException.class, () -> AstreaFlagResolver.unique(Collections.emptyList()));
        assertThrows(IllegalStateException.class, () -> AstreaFlagResolver.unique(Arrays.asList(
                Renamed.class.getDeclaredMethod("a", String.class),
                Renamed.class.getDeclaredMethod("z", String.class))));
    }
    @Test public void abstractStaticAndWrongReturnAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> AstreaFlagResolver.validate(
                AbstractGetter.class.getDeclaredMethod("read", String.class)));
        assertThrows(IllegalArgumentException.class, () -> AstreaFlagResolver.validate(
                Wrong.class.getDeclaredMethod("read", String.class)));
        assertThrows(IllegalArgumentException.class, () -> AstreaFlagResolver.validate(
                Wrong.class.getDeclaredMethod("typed", String.class)));
    }
}
