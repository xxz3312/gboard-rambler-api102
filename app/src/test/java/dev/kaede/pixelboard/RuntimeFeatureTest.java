package dev.kaede.pixelboard;

import android.content.Context;
import android.graphics.Paint;
import android.text.TextPaint;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public final class RuntimeFeatureTest {
    @Test public void resourceOnlySplitsDoNotEnterDexKit() throws Exception {
        for (String entry : new String[]{"resources.arsc", "classes.dex", "classes12.dex"}) {
            java.io.File apk=java.io.File.createTempFile("split-", ".apk");
            try {
                try (java.util.zip.ZipOutputStream zip=new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(apk))) {
                    zip.putNextEntry(new java.util.zip.ZipEntry(entry)); zip.write(0); zip.closeEntry();
                }
                assertEquals(!entry.equals("resources.arsc"), HostDex.containsDex(apk.getPath()));
            } finally { apk.delete(); }
        }
    }
    @Test public void unicodeDoesNotMisclassifyCjkSurrogatesOrPlainSymbols() {
        for (String text : new String[]{"中文", "𠮷", "𝄞", "→", "123", "©", "hello 😀", "😀 1", "\uD83D"})
            assertFalse(text, EmojiClassifier.isEmojiOnly(text));
        for (String text : new String[]{"😀", "👨‍👩‍👧‍👦", "🇨🇳", "1️⃣", "#⃣", "©️", "👍🏽", "❤️"})
            assertTrue(text, EmojiClassifier.isEmojiOnly(text));
        assertFalse(EmojiClassifier.containsEmoji("𠮷"));
        assertFalse(EmojiClassifier.containsEmoji(null));
    }
    @Test public void drawingRestoresHostAlphaOnExceptionsAndNestedDraws() {
        TextPaint paint = new TextPaint();
        paint.setColor(0x55776655);
        try {
            try (CandidatePaintScope scope = new CandidatePaintScope(true)) {
                CandidatePaintScope.afterPaintChange(paint);
                assertEquals(255, paint.getAlpha());
                try (CandidatePaintScope nested = new CandidatePaintScope(false)) {
                    TextPaint other = new TextPaint(); other.setAlpha(90);
                    CandidatePaintScope.afterPaintChange(other);
                    assertEquals(90, other.getAlpha());
                }
                paint.setAlpha(80); CandidatePaintScope.afterPaintChange(paint);
                assertEquals(255, paint.getAlpha());
                throw new IllegalStateException("host drawing exception");
            }
        } catch (IllegalStateException expected) {}
        assertEquals(80, paint.getAlpha());
        assertEquals(0x776655, paint.getColor() & 0xFFFFFF);
        paint.setAlpha(40); CandidatePaintScope.afterPaintChange(paint);
        assertEquals(40, paint.getAlpha());
        Paint background = new Paint(); background.setAlpha(30);
        try (CandidatePaintScope scope = new CandidatePaintScope(true)) {
            CandidatePaintScope.afterPaintChange(background);
            assertEquals(30, background.getAlpha());
        }
    }
    @Test public void retentionUsesLongArithmeticAndStockRestoration() {
        assertEquals(123L, ClipboardRetention.millis("stock",123L));
        assertEquals(123L, ClipboardRetention.millis("bad",123L));
        assertEquals(3_600_000L, ClipboardRetention.millis("1",123L));
        assertEquals(604_800_000L, ClipboardRetention.millis("168",123L));
        assertEquals(2_592_000_000L, ClipboardRetention.millis("720",123L));
        assertEquals(3_153_600_000_000L, ClipboardRetention.millis("876000",123L));
    }
    @Test public void nativeSettingsReadsAreNeverOverriddenOutsideCalculationScope() {
        String key=ClipboardRetention.NATIVE_KEY;
        assertEquals("24",RetentionReadScope.override(key,"24","720"));
        try {
            try (RetentionReadScope scope=new RetentionReadScope()) {
                assertEquals("720",RetentionReadScope.override(key,"24","720"));
                assertEquals("24",RetentionReadScope.override(key,"24","stock"));
                assertEquals("24",RetentionReadScope.override("other","24","720"));
                try (RetentionReadScope nested=new RetentionReadScope()) { assertTrue(RetentionReadScope.active()); }
                assertTrue(RetentionReadScope.active());
                throw new IllegalArgumentException("host error");
            }
        } catch (IllegalArgumentException expected) {}
        assertFalse(RetentionReadScope.active());
        assertEquals("24",RetentionReadScope.override(key,"24","720"));
    }
    @Test public void liveReloadSupportsEnablingAfterStartupAndDisabling() {
        Context context = RuntimeEnvironment.getApplication();
        android.content.SharedPreferences prefs = context.getSharedPreferences("live_test",0);
        prefs.edit().clear().commit(); FeatureConfig.reload(prefs);
        assertFalse(FeatureConfig.enabled(UpstreamFeature.GLIDE_TYPING));
        prefs.edit().putBoolean("glide_typing",true).putString(ClipboardRetention.KEY,"720").commit();
        FeatureConfig.reload(prefs);
        assertTrue(FeatureConfig.enabled(UpstreamFeature.GLIDE_TYPING));
        assertFalse(FeatureConfig.enabled(UpstreamFeature.AUTO_CORRECTION));
        assertEquals("720",FeatureConfig.retention());
        prefs.edit().clear().commit(); FeatureConfig.reload(prefs);
        assertFalse(FeatureConfig.enabled(UpstreamFeature.GLIDE_TYPING));
        assertEquals("stock",FeatureConfig.retention());
        Map<String,Object> malformed = new HashMap<>();
        malformed.put("glide_typing","true"); malformed.put(ClipboardRetention.KEY,Long.MAX_VALUE);
        FeatureConfig.Snapshot snapshot = new FeatureConfig.Snapshot(malformed);
        assertFalse(snapshot.has("glide_typing")); assertEquals("stock",snapshot.retention);
    }
    @Test public void policyGroupsStayIndependentAndPreserveTypes() {
        assertEquals(true, UpstreamFeature.transform("enable_two_stage_decoder", false, f -> f==UpstreamFeature.GLIDE_TYPING));
        assertEquals(false, UpstreamFeature.transform("allow_incognito_mode_switch", false, f -> f==UpstreamFeature.GLIDE_TYPING));
        assertEquals(150L, UpstreamFeature.transform("delay_to_wait_for_suggestions_before_commit_ms", 10L, f -> f==UpstreamFeature.AUTO_CORRECTION));
        assertEquals(10, UpstreamFeature.transform("delay_to_wait_for_suggestions_before_commit_ms", 10, f -> true));
        assertEquals(false, UpstreamFeature.transform("allow_incognito_mode_switch", false, f -> false));
    }
}
