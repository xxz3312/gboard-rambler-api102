package dev.kaede.pixelboard;

import android.view.View;
import android.widget.FrameLayout;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public final class RamblerUiSessionTest {
    @Test public void nestedKeyScopesKeepClosePurposeAndUnwindOnFailure() {
        Object header=new Object(), widget=new Object();
        try {
            try(RamblerUiSession.Scope scope=new RamblerUiSession.Scope(header,RamblerUiSession.Purpose.MINIMIZE)) {
                assertSame(scope,RamblerUiSession.Scope.current(header));
                scope.closeDepth++; assertFalse(scope.interceptFactory());
                try(RamblerUiSession.Scope nested=new RamblerUiSession.Scope(widget,RamblerUiSession.Purpose.KEYBOARD)) {
                    assertNull(RamblerUiSession.Scope.current(header));
                    assertTrue(nested.interceptFactory());
                }
                assertSame(scope,RamblerUiSession.Scope.current(header));
                scope.closeDepth--; assertTrue(scope.interceptFactory());
                throw new IllegalStateException("native callback failure");
            }
        } catch(IllegalStateException expected) {}
        assertNull(RamblerUiSession.Scope.current(header));
        assertNull(RamblerUiSession.Scope.current(widget));
    }
    @Test public void oldControllerTeardownCannotRemoveNewControllersKey() {
        RamblerUiSession session=new RamblerUiSession(); Object old=new Object(), active=new Object();
        session.placed(old,123); session.placed(active,123);
        assertEquals(0,session.cleared(old));
        assertEquals(123,session.slot(active));
        assertEquals(123,session.cleared(active));
        assertEquals(0,session.cleared(active));
        assertEquals(0,session.slot(null));
    }
    @Test public void revealOnlyChangesTheOwnedNativeSlot() {
        RamblerUiSession session=new RamblerUiSession(); Object owner=new Object();
        FrameLayout header=new FrameLayout(RuntimeEnvironment.getApplication());
        View minimize=new View(header.getContext()), other=new View(header.getContext());
        minimize.setId(123); other.setId(124); header.addView(minimize); header.addView(other);
        minimize.setAlpha(0f); minimize.setVisibility(View.GONE); other.setAlpha(.2f);
        assertFalse(session.reveal(owner,header));
        session.placed(owner,123); assertFalse(session.reveal(new Object(),header));
        assertTrue(session.reveal(owner,header));
        assertEquals(View.VISIBLE,minimize.getVisibility()); assertEquals(1f,minimize.getAlpha(),0f);
        assertEquals(.2f,other.getAlpha(),0f);
        session.cleared(owner); minimize.setAlpha(.3f);
        assertFalse(session.reveal(owner,header)); assertEquals(.3f,minimize.getAlpha(),0f);
    }
}
