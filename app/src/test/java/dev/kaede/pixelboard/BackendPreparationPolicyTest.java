package dev.kaede.pixelboard;
import org.junit.Test;
import static org.junit.Assert.*;

public class BackendPreparationPolicyTest {
    @Test public void localPromptCanPreparePcsWithoutGboardV2OrBindingPreflight() {
        assertTrue(BackendPreparationPolicy.independentPcsPreparation("com.google.android.as.oss", true));
    }
    @Test public void disabledExperimentRetainsStockBehavior() {
        assertFalse(BackendPreparationPolicy.independentPcsPreparation("com.google.android.as.oss", false));
    }
    @Test public void localOptInDoesNotEnableGboardOrModifyAicore() {
        assertFalse(BackendPreparationPolicy.independentPcsPreparation("com.google.android.inputmethod.latin", true));
        assertFalse(BackendPreparationPolicy.independentPcsPreparation("com.google.android.aicore", true));
        assertFalse(BackendPreparationPolicy.independentPcsPreparation(null, true));
    }
}
