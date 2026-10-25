package dev.kaede.pixelboard;
import org.junit.Test;
import static org.junit.Assert.*;
public final class CloudRuntimeStatusTest {
    @Test public void disabledFeaturesDoNotCountAsFailed() {
        assertFalse(CloudRuntimeStatus.installationFailed("ramblerRequested=false writingRequested=false ramblerInstalled=false writingInstalled=false getterInstalled=false"));
    }
    @Test public void extraFeaturesReportMissingGetter() {
        assertTrue(CloudRuntimeStatus.installationFailed("extrasRequested=true extrasInstalled=false"));
        assertFalse(CloudRuntimeStatus.installationFailed("extrasRequested=true extrasInstalled=true"));
        assertFalse(CloudRuntimeStatus.installationFailed("extrasRequested=false extrasInstalled=false"));
    }
    @Test public void eachRequestedFeatureMustBeInstalled() {
        assertTrue(CloudRuntimeStatus.installationFailed("ramblerRequested=true writingRequested=false ramblerInstalled=false writingInstalled=false"));
        assertTrue(CloudRuntimeStatus.installationFailed("ramblerRequested=false writingRequested=true ramblerInstalled=false writingInstalled=false"));
        assertFalse(CloudRuntimeStatus.installationFailed("ramblerRequested=true writingRequested=true ramblerInstalled=true writingInstalled=true"));
    }
}

