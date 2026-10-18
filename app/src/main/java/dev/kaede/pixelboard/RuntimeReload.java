package dev.kaede.pixelboard;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/** Only called by the explicit Root reload button. Targets are constant. */
public final class RuntimeReload {
    private RuntimeReload() {}
    public static String run() {
        Process process = null;
        try {
            String script = "set -e\n"
                    + "for target_package in com.google.android.inputmethod.latin com.google.android.aicore com.google.android.as.oss; do\n"
                    + "  before=$(pidof \"$target_package\" || true)\n"
                    + "  am force-stop \"$target_package\"\n"
                    + "  after=$(pidof \"$target_package\" || true)\n"
                    + "  printf '%s before=%s after=%s\\n' \"$target_package\" \"$before\" \"$after\"\n"
                    + "done\n";
            process = new ProcessBuilder("su", "-c", script).redirectErrorStream(true).start();
            if (!process.waitFor(25, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return "Root 重载超时；未确认进程停止。";
            }
            byte[] bytes = new byte[4096];
            int count = process.getInputStream().read(bytes);
            if (count < 0) count = 0;
            return "exit=" + process.exitValue() + "\n" + new String(bytes, 0, count, StandardCharsets.UTF_8)
                    + "\n进程停止结果不代表模块已加载；请查看实时应答。";
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            return "Root 重载失败：" + e.getClass().getSimpleName();
        } finally { if (process != null) process.destroy(); }
    }
}
