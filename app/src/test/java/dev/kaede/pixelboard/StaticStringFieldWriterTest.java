package dev.kaede.pixelboard;

import org.junit.Test;
import static org.junit.Assert.*;

public class StaticStringFieldWriterTest {
    static class Fixture {
        static final String MODEL = new String("original-model");
        static final String DEVICE = new String("original-device");
        static final String FINGERPRINT = new String("untouched");
        static int COUNT = 3;
        String instanceField = "instance";
    }
    private static Object read(String name) throws Exception {
        java.lang.reflect.Field field = Fixture.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }
    @Test public void writesFinalStringUsingDerivedSlotAndRefusesUnlistedField() throws Exception {
        StaticStringFieldWriter writer = StaticStringFieldWriter.create(Fixture.class,
                new String[] {"MODEL", "DEVICE"});
        String original = (String) read("MODEL");
        try {
            writer.set("MODEL", "test-model");
            assertEquals("test-model", read("MODEL"));
            assertEquals("original-device", read("DEVICE"));
            try { writer.set("FINGERPRINT", "changed"); fail("unlisted field accepted"); }
            catch (IllegalArgumentException expected) { }
            assertEquals("untouched", read("FINGERPRINT"));
        } finally {
            writer.set("MODEL", original);
        }
        assertEquals(original, read("MODEL"));
    }
    @Test public void rejectsBadFieldTypesAndIncompleteProfilesBeforeAnyWrite() throws Exception {
        for (String bad : new String[] {"COUNT", "instanceField", "MISSING"}) {
            try {
                StaticStringFieldWriter.create(Fixture.class, new String[] {"MODEL", bad});
                fail("invalid profile accepted");
            } catch (ReflectiveOperationException | IllegalArgumentException expected) { }
            assertEquals("original-model", read("MODEL"));
        }
    }
}
