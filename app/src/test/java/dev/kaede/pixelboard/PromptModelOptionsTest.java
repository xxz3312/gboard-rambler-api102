package dev.kaede.pixelboard;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.HashSet;
public final class PromptModelOptionsTest {
    @Test public void allFourConfigurationsAreDistinctAndDefaultMatchesSdk() {
        HashSet<String> combinations=new HashSet<>();
        for(int i=0;i<4;i++) {
            com.google.mlkit.genai.prompt.ModelConfig c=PromptModelOptions.config(i);
            combinations.add(c.getReleaseStage()+"/"+c.getPreference());
        }
        assertEquals(4,combinations.size());
        assertEquals(com.google.mlkit.genai.prompt.ModelConfig.getDEFAULT(),PromptModelOptions.config(0));
    }
    @Test public void refusesUnknownConfiguration() {
        for(int i:new int[]{-1,4})try { PromptModelOptions.config(i);fail(); }
        catch(IllegalArgumentException expected) {}
    }
}
