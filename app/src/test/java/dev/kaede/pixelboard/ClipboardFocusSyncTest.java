package dev.kaede.pixelboard;

import android.content.ClipboardManager;
import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public final class ClipboardFocusSyncTest {
    private EditorInfo editor(int type) { EditorInfo e=new EditorInfo(); e.inputType=type; return e; }
    @Test public void registeredCallbacksDeduplicateThrottleAndUnregister() {
        ClipboardFocusSync sync=new ClipboardFocusSync(); int[] count={0};
        ClipboardManager.OnPrimaryClipChangedListener listener=() -> count[0]++;
        sync.add(listener); sync.add(listener);
        assertEquals(1,sync.dispatch(editor(InputType.TYPE_CLASS_TEXT),0));
        assertEquals(0,sync.dispatch(editor(InputType.TYPE_CLASS_TEXT),100));
        assertEquals(1,sync.dispatch(editor(InputType.TYPE_CLASS_TEXT),1600));
        sync.remove(listener);
        assertEquals(0,sync.dispatch(editor(InputType.TYPE_CLASS_TEXT),3200));
        assertEquals(2,count[0]);
    }
    @Test public void privateAndPasswordEditorsDoNotTriggerNativeCallbacks() {
        ClipboardFocusSync sync=new ClipboardFocusSync(); int[] count={0};
        ClipboardManager.OnPrimaryClipChangedListener listener=() -> count[0]++;
        sync.add(listener);
        for (int type:new int[]{InputType.TYPE_NULL,
                InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD,
                InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
                InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD})
            assertEquals(0,sync.dispatch(editor(type),10000));
        EditorInfo privateEditor=editor(InputType.TYPE_CLASS_TEXT);
        privateEditor.imeOptions=EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING;
        assertEquals(0,sync.dispatch(privateEditor,10000));
        assertEquals(0,count[0]);
    }
    @Test public void oneFailingCallbackDoesNotBreakRemainingCallbacks() {
        ClipboardFocusSync sync=new ClipboardFocusSync();
        ClipboardManager.OnPrimaryClipChangedListener failure=() -> { throw new IllegalStateException(); };
        ClipboardManager.OnPrimaryClipChangedListener success=() -> {};
        sync.add(failure); sync.add(success);
        assertEquals(1,sync.dispatch(editor(InputType.TYPE_CLASS_TEXT),0));
    }
}
