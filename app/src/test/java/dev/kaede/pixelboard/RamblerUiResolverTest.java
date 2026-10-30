package dev.kaede.pixelboard;

import android.view.View;
import android.view.ViewGroup;
import java.lang.reflect.Method;
import org.junit.Test;
import static org.junit.Assert.*;

public final class RamblerUiResolverTest {
    static class Key {}
    static class Builder { void z(int id) {} Key k() { return new Key(); } }
    static class Base {
        Key f(int icon,Runnable action) { return new Key(); }
        Builder b(int icon,Runnable action) { return new Builder(); }
        Key c(int icon) { return new Key(); } int i() { return 1; }
        void t(Object state,View first,View second) {}
    }
    static class Header extends Base { Key q() { return new Key(); } void h(ViewGroup group,boolean animate) {} }
    static class Widget extends Base { Key q() { return new Key(); } }
    static class Bus { static void a(int id,Key key) {} static void r(int id,String key) {} }
    static class Wrong { static Key f(int icon,Runnable action) { return null; } }
    private Method[] family() throws Exception {
        return new Method[]{Header.class.getDeclaredMethod("q"),Widget.class.getDeclaredMethod("q"),
                Base.class.getDeclaredMethod("f",int.class,Runnable.class),Base.class.getDeclaredMethod("b",int.class,Runnable.class),
                Builder.class.getDeclaredMethod("z",int.class),Builder.class.getDeclaredMethod("k"),
                Base.class.getDeclaredMethod("c",int.class),Base.class.getDeclaredMethod("i"),
                Base.class.getDeclaredMethod("t",Object.class,View.class,View.class),
                Header.class.getDeclaredMethod("h",ViewGroup.class,boolean.class),
                Bus.class.getDeclaredMethod("a",int.class,Key.class),Bus.class.getDeclaredMethod("r",int.class,String.class)};
    }
    @Test public void renamedNativeFamilyIsAcceptedWithoutVersionOrSymbolNames() throws Exception {
        RamblerUiResolver.validate(family());
    }
    @Test public void ambiguousControllerOrStaticFactoryIsRejected() throws Exception {
        Method[] methods=family(); methods[1]=methods[0];
        assertThrows(IllegalArgumentException.class,()->RamblerUiResolver.validate(methods));
        Method[] wrong=family(); wrong[2]=Wrong.class.getDeclaredMethod("f",int.class,Runnable.class);
        assertThrows(IllegalArgumentException.class,()->RamblerUiResolver.validate(wrong));
    }
    @Test public void missingResourcesCannotInstallPartialBinding() throws Exception {
        Method[] methods=family();
        assertThrows(IllegalArgumentException.class,()->new RamblerUiResolver.Binding(methods,0,12,13));
        assertThrows(IllegalArgumentException.class,()->new RamblerUiResolver.Binding(methods,11,0,13));
    }
}
