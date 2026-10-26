package dev.kaede.pixelboard;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/** User-initiated text export. Root is used only when selected in the export dialog. */
public final class DiagnosticReport {
    private DiagnosticReport() {}
    public static String build(Context context, boolean rootLogs) {
        StringBuilder report=new StringBuilder("Gboard Rambler 诊断日志\n");
        report.append("导出时间：").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z",java.util.Locale.US)
                .format(new java.util.Date())).append('\n');
        report.append("模块版本：").append(CloudRuntimeStatus.VERSION).append('\n');
        report.append("设备：").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
                .append(" / ").append(Build.DEVICE).append('\n');
        report.append("Android：").append(Build.VERSION.RELEASE).append(" / API ").append(Build.VERSION.SDK_INT).append('\n');
        try {
            PackageInfo pkg=context.getPackageManager().getPackageInfo("com.google.android.inputmethod.latin",0);
            report.append("Gboard：").append(pkg.versionName).append(" (").append(pkg.getLongVersionCode()).append(")\n");
        } catch (Exception error) { report.append("Gboard：未安装或版本不可读取\n"); }
        report.append("\n配置与同步\n");
        Context app=context.getApplicationContext();
        if (app instanceof ModuleApp) report.append(((ModuleApp)app).configurationStatus()).append('\n');
        report.append("\n适配与运行状态\n").append(CloudRuntimeStatus.report(context));
        report.append("\n权限状态\n").append(HostFeaturePermissions.summary(context)).append('\n');
        report.append("\n历史事件（包含此前进程/版本）\n").append(DiagnosticLog.history(context));
        report.append("\n模块 logcat（").append(rootLogs?"Root，包含 Gboard 进程":"当前应用权限，可见范围有限").append("）\n");
        report.append(captureLogcat(rootLogs));
        return report.toString();
    }
    static String captureLogcat(boolean rootLogs) {
        java.lang.Process process=null;
        try {
            ProcessBuilder builder=rootLogs
                    ? new ProcessBuilder("su","-c","logcat -d -v threadtime -t 1200 -s 'PixelBoardLS:*'")
                    : new ProcessBuilder("logcat","-d","-v","threadtime","-t","1200","-s","PixelBoardLS:*");
            process=builder.redirectErrorStream(true).start();
            final InputStream stream=process.getInputStream();
            FutureTask<String> output=new FutureTask<>(() -> {
                try (Reader reader=new InputStreamReader(stream,StandardCharsets.UTF_8)) {
                    StringBuilder text=new StringBuilder(); char[] buffer=new char[4096]; int count;
                    while ((count=reader.read(buffer))!=-1) {
                        int remaining=131072-text.length();
                        if (remaining>0) text.append(buffer,0,Math.min(remaining,count));
                    }
                    return text.toString();
                }
            });
            Thread reader=new Thread(output,"diagnostic-logcat-reader"); reader.setDaemon(true); reader.start();
            if (!process.waitFor(12,TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return "日志读取或 Root 授权超时；以上诊断报告仍可使用。\n";
            }
            String text=output.get(2,TimeUnit.SECONDS);
            if (process.exitValue()!=0) return "日志读取失败（可能未授权 Root）。\n"+text;
            return text.trim().isEmpty() ? (rootLogs
                    ? "Root 读取完成，但没有可见的 PixelBoardLS 日志；请结合上面的运行事件判断。\n"
                    : "没有可见的模块 logcat；可选择 Root 导出补充跨进程日志。\n") : text;
        } catch (Exception error) {
            if (error instanceof InterruptedException) Thread.currentThread().interrupt();
            return "无法读取 logcat："+error.getClass().getSimpleName()+"；以上诊断报告仍可使用。\n";
        } finally { if (process!=null) process.destroy(); }
    }
}
