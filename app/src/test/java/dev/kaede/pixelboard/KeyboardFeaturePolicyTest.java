package dev.kaede.pixelboard;
import org.junit.Test;
import static org.junit.Assert.*;
public final class KeyboardFeaturePolicyTest {
    @Test public void disabledRestoresOriginal() {
        assertEquals(false, value("enable_ocr",false,false,false,false));
        assertEquals(true, value("belka_ui",true,false,false,false));
        assertEquals("en", value("enabled_ocr_language_tags","en",false,false,false));
    }
    @Test public void togglesAreIndependent() {
        assertEquals(true,value("enable_ocr",false,true,false,false));
        assertEquals(false,value("belka_ui",false,true,false,false));
        assertEquals(false,value("enable_screenshot_chip",false,true,false,false));
        assertEquals(true,value("enable_screenshot_chip",false,false,true,false));
        assertEquals(false,value("enable_ocr",false,false,true,false));
        assertEquals(true,value("belka_ui",false,false,false,true));
        assertEquals("*",value("enabled_ocr_language_tags","en",true,false,false));
    }
    @Test public void unknownAndUnexpectedTypesArePreserved() {
        assertEquals(7L,value("config_ocr",7L,true,true,true));
        assertEquals("proto",value("config_ocr","proto",true,true,true));
        assertNull(value("enable_ocr",null,true,true,true));
        assertEquals(false,value("ocr_password_allowlist",false,true,true,true));
        assertEquals(false,value("writing_tools",false,true,true,true));
        assertEquals(false,value(null,false,true,true,true));
    }
    private Object value(String key,Object original,boolean ocr,boolean shot,boolean round) {
        return KeyboardFeaturePolicy.overrideValue(key,original,ocr,shot,round);
    }
}
