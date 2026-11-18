package dev.kaede.pixelboard;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public final class RamblerRotationGraceTest {
    private static final int PORTRAIT = 1;
    private static final int LANDSCAPE = 2;

    @After
    public void tearDown() {
        RamblerRotationGrace.resetForTests();
    }

    @Test
    public void noGraceBeforeAnyDisplayChange() {
        RamblerRotationGrace.onDisplayConfiguration(PORTRAIT, 475, 751, 1_000L);
        Assert.assertEquals(0L, RamblerRotationGrace.deactivationDelayMs(1_010L));
    }

    @Test
    public void rotationGrantsGraceInsideTheWindowOnly() {
        RamblerRotationGrace.onDisplayConfiguration(PORTRAIT, 475, 751, 1_000L);
        RamblerRotationGrace.onDisplayConfiguration(LANDSCAPE, 751, 475, 5_000L);

        // Rotation deactivates Rambler twice within ~150 ms; both must keep the session.
        Assert.assertEquals(RamblerRotationGrace.DEACTIVATION_GRACE_MS,
                RamblerRotationGrace.deactivationDelayMs(5_019L));
        Assert.assertEquals(RamblerRotationGrace.DEACTIVATION_GRACE_MS,
                RamblerRotationGrace.deactivationDelayMs(5_138L));
        Assert.assertEquals(RamblerRotationGrace.DEACTIVATION_GRACE_MS,
                RamblerRotationGrace.deactivationDelayMs(
                        5_000L + RamblerRotationGrace.DISPLAY_CHANGE_WINDOW_MS));
        // Once the window passes, closing the keyboard stops Rambler instantly again.
        Assert.assertEquals(0L, RamblerRotationGrace.deactivationDelayMs(
                5_000L + RamblerRotationGrace.DISPLAY_CHANGE_WINDOW_MS + 1L));
    }

    @Test
    public void laterRotationStartsANewWindow() {
        RamblerRotationGrace.onDisplayConfiguration(PORTRAIT, 475, 751, 1_000L);
        RamblerRotationGrace.onDisplayConfiguration(LANDSCAPE, 751, 475, 5_000L);
        Assert.assertEquals(0L, RamblerRotationGrace.deactivationDelayMs(30_000L));

        RamblerRotationGrace.onDisplayConfiguration(PORTRAIT, 475, 751, 30_000L);
        Assert.assertEquals(RamblerRotationGrace.DEACTIVATION_GRACE_MS,
                RamblerRotationGrace.deactivationDelayMs(30_020L));
    }

    @Test
    public void unrelatedConfigurationChangesGrantNoGrace() {
        RamblerRotationGrace.onDisplayConfiguration(PORTRAIT, 475, 751, 1_000L);
        // Same display shape, e.g. a locale, theme or font scale change.
        RamblerRotationGrace.onDisplayConfiguration(PORTRAIT, 475, 751, 5_000L);
        Assert.assertEquals(0L, RamblerRotationGrace.deactivationDelayMs(5_010L));
    }

    @Test
    public void screenSizeChangeWithoutOrientationChangeGrantsGrace() {
        RamblerRotationGrace.onDisplayConfiguration(PORTRAIT, 475, 751, 1_000L);
        RamblerRotationGrace.onDisplayConfiguration(PORTRAIT, 600, 751, 5_000L);
        Assert.assertEquals(RamblerRotationGrace.DEACTIVATION_GRACE_MS,
                RamblerRotationGrace.deactivationDelayMs(5_010L));
    }
}
