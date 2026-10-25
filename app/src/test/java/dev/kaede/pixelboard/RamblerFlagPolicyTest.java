package dev.kaede.pixelboard;

import org.junit.Test;
import static org.junit.Assert.*;

public final class RamblerFlagPolicyTest {
    @Test public void removesDelayOnlyForEnabledRamblerAndCorrectType() {
        String flag="agentic_dictation_deactivation_delay_ms";
        assertTrue(RamblerFlagPolicy.targeted(flag));
        assertEquals(0L,RamblerFlagPolicy.overrideValue(flag,500L,true));
        assertEquals(500L,RamblerFlagPolicy.overrideValue(flag,500L,false));
        assertEquals(500,RamblerFlagPolicy.overrideValue(flag,500,true));
        assertEquals(Boolean.TRUE,RamblerFlagPolicy.overrideValue(flag,Boolean.TRUE,true));
    }
    @Test public void leavesPrunedAnimationFlagsStock() {
        for(String flag:new String[]{"enable_rambler_toolbar_at_cursor_position",
                "enable_hide_rambler_al_idling_state","enable_rambler_al_show_hide_new_animation",
                "enable_rambler_al_teleporting_animation"}) {
            assertFalse(RamblerFlagPolicy.targeted(flag));
            assertSame(Boolean.FALSE,RamblerFlagPolicy.overrideValue(flag,Boolean.FALSE,true));
        }
    }
}
