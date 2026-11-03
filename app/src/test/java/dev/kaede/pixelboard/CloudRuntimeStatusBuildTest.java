package dev.kaede.pixelboard;

import android.content.Context;
import android.content.SharedPreferences;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public final class CloudRuntimeStatusBuildTest {
    @Test public void staleFailureFromSameVersionDifferentBuildIsNotCurrent() {
        Context context = RuntimeEnvironment.getApplication();
        SharedPreferences p = context.getSharedPreferences("cloud_runtime_status", 0);
        p.edit().clear().putString("version", CloudRuntimeStatus.VERSION)
                .putInt("build", CloudRuntimeStatus.BUILD - 1)
                .putString("COMPAT", "rambler=false writing=true resolver=cache")
                .putString("HOOKS", "ramblerRequested=true ramblerInstalled=false")
                .putString("RUNTIME_HOOKS", "retentionCalculation=skipped").commit();
        assertFalse(CloudRuntimeStatus.unsupported(context));
        assertFalse(CloudRuntimeStatus.retentionUnavailable(context));
        String report = CloudRuntimeStatus.report(context);
        assertTrue(report.contains("旧构建记录不用于判断兼容性"));
        assertFalse(report.contains("rambler=false"));
        assertFalse(report.contains("Hook 安装未完成"));
    }
    @Test public void currentBuildStillReportsRealFailuresAndRecovery() {
        Context context = RuntimeEnvironment.getApplication();
        SharedPreferences p = context.getSharedPreferences("cloud_runtime_status", 0);
        p.edit().clear().putString("version", CloudRuntimeStatus.VERSION)
                .putInt("build", CloudRuntimeStatus.BUILD)
                .putString("COMPAT", "rambler=false writing=true")
                .putString("HOOKS", "ramblerRequested=true ramblerInstalled=false").commit();
        assertTrue(CloudRuntimeStatus.unsupported(context));
        assertTrue(CloudRuntimeStatus.report(context).contains("本次初始化未找到 Rambler 选择器"));
        p.edit().putString("COMPAT", "rambler=true writing=true")
                .putString("HOOKS", "ramblerRequested=true ramblerInstalled=true").commit();
        assertFalse(CloudRuntimeStatus.unsupported(context));
        assertTrue(CloudRuntimeStatus.report(context).contains("Hook 安装检查完成"));
    }
    @Test public void legacyAndOtherVersionEventsAreNotCurrent() {
        assertFalse(CloudRuntimeStatus.sameBuild(CloudRuntimeStatus.VERSION, -1));
        assertFalse(CloudRuntimeStatus.sameBuild("0.0.1", CloudRuntimeStatus.BUILD));
        assertTrue(CloudRuntimeStatus.sameBuild(CloudRuntimeStatus.VERSION, CloudRuntimeStatus.BUILD));
    }
}
