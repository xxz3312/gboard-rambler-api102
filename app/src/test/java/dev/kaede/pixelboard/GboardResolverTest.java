package dev.kaede.pixelboard;

import org.junit.Test;
import static org.junit.Assert.*;
import java.lang.reflect.*;
import java.util.*;

public final class GboardResolverTest {
    public static class RenamedFlag {
        String renamedKey; Class<?> renamedType;
        public RenamedFlag(String key,Class<?> type) { renamedKey=key; renamedType=type; }
        public Object completelyDifferentName() { return Boolean.TRUE; }
        public Object wrongSignature(int value) { return value; }
        public static Object staticGetter() { return Boolean.TRUE; }
    }
    public static class AmbiguousFlag {
        String first,second; Class<?> kind;
        public AmbiguousFlag(String key,Class<?> type) {}
        public Object get() { return null; }
    }
    public static class UntypedFlag {
        String key;
        public UntypedFlag(String key,Class<?> type) {}
        public Object get() { return null; }
    }
    @Test public void acceptsRenamedGetterAndFieldWithoutVersion() throws Exception {
        Method method=RenamedFlag.class.getDeclaredMethod("completelyDifferentName");
        RamblerFlagBindings.Binding bound=GboardResolver.binding(method,"fixture");
        assertEquals("writing_tools",bound.name(new RenamedFlag("writing_tools",Boolean.class)));
        assertEquals(method,bound.getter);
    }
    @Test public void rejectsStaticAndWrongParameters() throws Exception {
        for(Method method:RenamedFlag.class.getDeclaredMethods()) {
            if(method.getName().equals("completelyDifferentName"))continue;
            try { GboardResolver.binding(method,"fixture"); fail("Accepted wrong signature"); }
            catch(IllegalArgumentException expected) {}
        }
    }
    @Test public void refusesAmbiguousNameField() throws Exception {
        try { GboardResolver.binding(AmbiguousFlag.class.getDeclaredMethod("get"),"fixture"); fail(); }
        catch(IllegalArgumentException expected) {}
    }
    @Test public void requiresTypeDescriptorField() throws Exception {
        try { GboardResolver.binding(UntypedFlag.class.getDeclaredMethod("get"),"fixture"); fail(); }
        catch(IllegalArgumentException expected) {}
    }
    @Test public void zeroOrMultipleCandidatesNeverPickFirst() {
        for(List<String> values:Arrays.asList(Collections.<String>emptyList(),Arrays.asList("one","two"))) {
            try { GboardResolver.unique(values); fail("Ambiguous selection accepted"); }
            catch(IllegalArgumentException expected) {}
        }
        assertEquals("only",GboardResolver.unique(Collections.singletonList("only")));
    }
    @Test public void negativeSelectorCacheIsAlwaysRetried() {
        assertFalse(GboardResolver.cacheReusable("same-apk", "same-apk", false));
        assertTrue(GboardResolver.cacheReusable("same-apk", "same-apk", true));
        assertFalse(GboardResolver.cacheReusable("new-apk", "old-apk", true));
    }
    @Test public void selectorFailureNamesTheFailedStage() {
        try {
            GboardResolver.selectorUnique(Collections.emptyList(), "read");
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals("selector stage=read matches=0", expected.getMessage());
        }
        try {
            GboardResolver.selectorUnique(Arrays.asList("one", "two"), "setup");
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals("selector stage=setup matches=2", expected.getMessage());
        }
    }
}
