package dev.kaede.pixelboard;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public final class UpdateCheckerTest {
    private String release(String tag) {
        return "{\"tag_name\":\""+tag+"\",\"draft\":false,\"prerelease\":false,\"body\":\"更新说明\",\"html_url\":\"https://example.com/fake.apk\"}";
    }
    @Test public void comparesNumericSegmentsAndDoesNotOfferDowngrades() throws Exception {
        assertTrue(UpdateChecker.compareVersions("v0.1.100","0.1.35")>0);
        assertTrue(UpdateChecker.compareVersions("0.1.9","0.1.35")<0);
        assertEquals(0,UpdateChecker.compareVersions("V0.1.35.0","0.1.35"));
        assertTrue(UpdateChecker.compareVersions("99999999999999999999","37")>0);
        assertEquals(UpdateChecker.Status.CURRENT,UpdateChecker.parse(release("v0.1.35"),"0.2").status);
        assertEquals(UpdateChecker.Status.CURRENT,UpdateChecker.parse(release("0.2"),"0.2").status);
        assertEquals(UpdateChecker.Status.NEWER,UpdateChecker.parse(release("0.2.1"),"0.2").status);
        assertEquals(UpdateChecker.Status.NEWER,UpdateChecker.parse(release("v0.1.36"),"0.1.35").status);
        assertEquals(UpdateChecker.Status.CURRENT,UpdateChecker.parse(release("0.1.35"),"0.1.35").status);
        assertEquals(UpdateChecker.Status.CURRENT,UpdateChecker.parse(release("v0.1.30"),"0.1.35").status);
    }
    @Test public void onlyBuildsLinksToTheProjectRelease() throws Exception {
        UpdateChecker.Result result=UpdateChecker.parse(release("v0.1.36"),"0.1.35");
        assertEquals("https://github.com/xxz3312/gboard-rambler-api102/releases/tag/v0.1.36",result.url);
        assertEquals("更新说明",result.notes);
        for (String tag:new String[]{"v0.1.36-rc1","../other","javascript:alert(1)","", "1e3"}) {
            try { UpdateChecker.parse(release(tag),"0.1.35"); fail(tag); }
            catch (IllegalArgumentException expected) {}
        }
    }
    @Test public void rejectsDraftPrereleaseAndMalformedResponses() {
        for (String json:new String[]{release("v0.1.36").replace("\"draft\":false","\"draft\":true"),
                release("v0.1.36").replace("\"prerelease\":false","\"prerelease\":true"),"{}","not json"}) {
            try { UpdateChecker.parse(json,"0.1.35"); fail(json); }
            catch (Exception expected) {}
        }
    }
    @Test public void boundedResponseKeepsUtf8AndRejectsLargeBodies() throws Exception {
        assertEquals("更新说明",UpdateChecker.readBody(new ByteArrayInputStream("更新说明".getBytes(StandardCharsets.UTF_8))));
        try { UpdateChecker.readBody(new ByteArrayInputStream(new byte[262145])); fail(); }
        catch (IOException expected) {}
    }
}
