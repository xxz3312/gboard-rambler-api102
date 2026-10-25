package dev.kaede.pixelboard;

import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35, manifest=Config.NONE)
public final class ModuleReleaseTest {
    private String json(String tag, String url) {
        return "{\"draft\":false,\"prerelease\":false,\"tag_name\":\"" + tag
                + "\",\"body\":\"更新说明\",\"assets\":[{\"name\":\"Gboard-Rambler-API102-0.1.31.apk\","
                + "\"size\":100,\"browser_download_url\":\"" + url + "\"}]}";
    }
    private final String apk = "https://github.com/xxz3312/gboard-rambler-api102/releases/download/v0.1.31/Gboard-Rambler-API102-0.1.31.apk";
    @Test public void numericComparisonHandlesTwoDigitPatchesAndFutureLocalBuilds() {
        assertTrue(ModuleRelease.compare("v0.1.31", "0.1.9") > 0);
        assertTrue(ModuleRelease.compare("0.2.0", "0.1.99") > 0);
        assertEquals(0, ModuleRelease.compare("v0.1.31", "0.1.31"));
        assertTrue(ModuleRelease.compare("0.1.30", "0.1.31") < 0);
    }
    @Test public void acceptsOnlyModuleApkFromOwnRelease() throws Exception {
        ModuleRelease result = ModuleRelease.parse(json("v0.1.31", apk));
        assertEquals("0.1.31", result.version);
        assertEquals(apk, result.apkUrl);
        assertTrue(result.pageUrl.endsWith("/releases/tag/v0.1.31"));
        assertEquals("更新说明", result.notes);
    }
    @Test public void rejectsExternalOrUpstreamUrlsAndMalformedResponses() {
        for (String url : new String[] {apk.replace("github.com", "github.com.evil.test"),
                apk.replace("xxz3312/gboard-rambler-api102", "Akshayykadam/PixelBoard"),
                apk.replace("https:", "http:"), apk + "?redirect=1"}) {
            try { ModuleRelease.parse(json("v0.1.31", url)); fail(url); }
            catch (IOException expected) { }
        }
        for (String value : new String[] {"{}", "not json", json("v0.1.31-rc1", apk),
                json("v0.1.31", apk).replace("\"prerelease\":false", "\"prerelease\":true"),
                json("v0.1.31", apk).replace("\"draft\":false", "\"draft\":true"),
                json("v0.1.31", apk).replace("Gboard-Rambler-API102-0.1.31.apk", "PixelBoard.apk")}) {
            try { ModuleRelease.parse(value); fail(value); }
            catch (IOException expected) { }
        }
    }
}
