package dev.kaede.pixelboard;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.mlkit.genai.common.FeatureStatus;
import com.google.mlkit.genai.rewriting.Rewriter;
import com.google.mlkit.genai.rewriting.RewriterOptions;
import com.google.mlkit.genai.rewriting.Rewriting;
import com.google.mlkit.genai.proofreading.Proofreader;
import com.google.mlkit.genai.proofreading.ProofreaderOptions;
import com.google.mlkit.genai.proofreading.Proofreading;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** Foreground, user-triggered capability queries in the module app only.
 * No inference, downloadFeature(), keyboard text access or backend changes.
 */
public final class AiCoreProbeActivity extends Activity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Future<?> job;
    private TextView report;
    private Button check;
    private volatile boolean active;
    private boolean busy;
    private int generation;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScrollView root = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        list.setPadding(pad, pad, pad, pad);
        root.addView(list);
        TextView title = new TextView(this);
        title.setText("V2 后端信息与公开 API 检测");
        title.setTextSize(22);
        list.addView(title);
        TextView explanation = new TextView(this);
        explanation.setText("检测 607/614 公开 API 状态，并显示后端包与绑定权限。"
                + "请保持此页在前台。检测不会主动下载模型或提交推理请求，也不会切换 Gboard 后端。"
                + "\nGboard V2 的私有推理另用 Feature 703；607/614 的结果不能判定它是否可用。");
        list.addView(explanation);
        check = new Button(this);
        check.setText("开始检测");
        check.setOnClickListener(v -> startCheck());
        list.addView(check);
        Button copy = new Button(this);
        copy.setText("复制检测结果");
        copy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("AICore 检测结果", report.getText()));
                Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show();
            }
        });
        list.addView(copy);
        report = new TextView(this);
        report.setTextIsSelectable(true);
        if (savedInstanceState != null && savedInstanceState.containsKey("report")) {
            report.setText(savedInstanceState.getString("report", "")
                    + "\n页面已恢复；如检测未完成，请重新检测。");
        } else {
            report.setText(deviceReport() + "\n点击开始检测仅查询公开 API，不验证 V2 推理。");
        }
        list.addView(report);
        setContentView(root);
    }

    @Override protected void onStart() {
        super.onStart();
        active = true;
    }

    private void startCheck() {
        if (busy || !active) return;
        busy = true;
        check.setEnabled(false);
        int token = ++generation;
        report.setText(deviceReport() + "\n检测中（每项最多等待 20 秒）…\n");
        job = executor.submit(() -> {
            if (!active || Thread.currentThread().isInterrupted()) return;
            probeRewrite(token);
            if (!active || Thread.currentThread().isInterrupted()) return;
            probeProofread(token);
            runOnUiThread(() -> {
                if (!active || token != generation || isDestroyed()) return;
                report.append("\n检测结束。AVAILABLE 只表示该 API 配置已就绪；"
                        + "DOWNLOADABLE 表示可请求下载；UNAVAILABLE 或异常需要进一步分析。"
                        + "\nV2 真实请求、来源及返回状态请以 Gboard 实际操作和 PixelBoardLS 日志为准。");
                busy = false;
                check.setEnabled(true);
            });
        });
    }

    private String deviceReport() {
        StringBuilder text = new StringBuilder();
        try {
            text.append("模块版本：").append(getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException ignored) { }
        text.append("\n");
        android.content.SharedPreferences settings = getSharedPreferences(FeatureConfig.GROUP, MODE_PRIVATE);
        boolean requested = settings.getBoolean("writing_tools", false)
                && settings.getBoolean("writing_tools_v2", false);
        text.append("保存配置：").append(requested ? "V2 实验，后端="
                + (settings.getInt("writing_tools_backend", 2) == 3 ? "Astrea" : "AICore") : "基础云端")
                .append("（Gboard 重启后读取，不代表有效运行状态）\n");
        text.append("设备：").append(Build.MANUFACTURER).append(" ")
                .append(Build.MODEL).append(" / ").append(Build.DEVICE)
                .append("\nAndroid API：").append(Build.VERSION.SDK_INT).append("\n");
        try {
            PackageInfo p = getPackageManager().getPackageInfo("com.google.android.aicore", 0);
            text.append("AICore：").append(p.versionName).append(" (")
                    .append(p.getLongVersionCode()).append(")")
                    .append("\n应用启用：")
                    .append(p.applicationInfo != null && p.applicationInfo.enabled);
        } catch (PackageManager.NameNotFoundException e) {
            text.append("AICore：未检测到安装包；仍继续查询 SDK 状态");
        } catch (RuntimeException e) {
            text.append("AICore 包查询异常：").append(describe(e));
        }
        appendPackage(text, "Gboard", "com.google.android.inputmethod.latin");
        appendPackage(text, "Astrea / Private Compute Services", "com.google.android.as.oss");
        appendAstreaService(text);
        appendBindingReport(text);
        WritingToolsBackendCheck.Result prerequisites = WritingToolsBackendCheck.check(this,
                settings.getInt("writing_tools_backend", 2));
        text.append("\nV2 绑定预检：").append(prerequisites.code)
                .append("\n").append(prerequisites.message)
                .append("\n最终生效模式仍以 Gboard 进程启动日志为准。");
        text.append("\n注意：官方文档不支持解锁 Bootloader 的设备；"
                + "服务初始化、机型、语言和模型配置也会影响结果。\n");
        return text.toString();
    }



    private void appendPackage(StringBuilder text, String title, String packageName) {
        try {
            PackageInfo p = getPackageManager().getPackageInfo(packageName, 0);
            text.append("\n").append(title).append("：").append(p.versionName)
                    .append(" (").append(p.getLongVersionCode()).append(")");
        } catch (PackageManager.NameNotFoundException | RuntimeException e) {
            text.append("\n").append(title).append("：未找到或不可见");
        }
    }

    private void appendAstreaService(StringBuilder text) {
        try {
            android.content.pm.ServiceInfo service = getPackageManager().getServiceInfo(
                    new android.content.ComponentName("com.google.android.as.oss",
                            "com.google.android.apps.miphone.astrea.grpc.AstreaGrpcService"),
                    PackageManager.MATCH_DISABLED_COMPONENTS);
            text.append("\nAstrea 服务：enabled=").append(service.enabled)
                    .append(", exported=").append(service.exported)
                    .append(", permission=").append(service.permission == null ? "(无)" : service.permission)
                    .append("（仅验证声明，不代表 RPC 或推理成功）");
        } catch (PackageManager.NameNotFoundException | RuntimeException e) {
            text.append("\nAstrea 服务：未找到或不可见");
        }
    }

    private void appendBindingReport(StringBuilder text) {
        PackageManager pm = getPackageManager();
        String bindingPermission = "com.google.android.apps.aicore.service.BIND_SERVICE";
        text.append("\n绑定权限诊断：");
        try {
            PackageInfo own = pm.getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS);
            boolean requested = own.requestedPermissions != null
                    && java.util.Arrays.asList(own.requestedPermissions).contains(bindingPermission);
            text.append("\n模块已声明 BIND_SERVICE：").append(requested);
        } catch (PackageManager.NameNotFoundException | RuntimeException e) {
            text.append("\n模块权限查询失败：").append(describe(e));
        }
        for (String className : new String[] {
                "com.google.android.apps.aicore.service.multiuser.AiCoreMultiUserService",
                "com.google.android.apps.aicore.service.AiCoreService"}) {
            try {
                android.content.pm.ServiceInfo service = pm.getServiceInfo(
                        new android.content.ComponentName("com.google.android.aicore", className),
                        PackageManager.MATCH_DISABLED_COMPONENTS);
                text.append("\n服务：").append(className)
                        .append("\n  enabled=").append(service.enabled)
                        .append(", exported=").append(service.exported)
                        .append("\n  要求权限：").append(service.permission == null ? "(无)" : service.permission);
                if (service.permission != null) appendPermissionReport(text, pm, service.permission);
            } catch (PackageManager.NameNotFoundException e) {
                text.append("\n服务未找到：").append(className);
            } catch (RuntimeException e) {
                text.append("\n服务查询失败：").append(describe(e));
            }
        }
        // Also report the known permission if no visible service descriptor exposed it.
        appendPermissionReport(text, pm, bindingPermission);
    }

    private void appendPermissionReport(StringBuilder text, PackageManager pm, String name) {
        try {
            android.content.pm.PermissionInfo permission = pm.getPermissionInfo(name, 0);
            text.append("\n  权限：").append(name)
                    .append("\n  定义包：").append(permission.packageName)
                    .append("\n  protectionLevel=0x")
                    .append(Integer.toHexString(permission.protectionLevel))
                    .append("（基础类型：").append(protectionBase(permission.getProtection())).append("）")
                    .append("\n  模块已获授权：")
                    .append(pm.checkPermission(name, getPackageName()) == PackageManager.PERMISSION_GRANTED)
                    .append("\n  Gboard 已获授权：")
                    .append(pm.checkPermission(name, "com.google.android.inputmethod.latin")
                            == PackageManager.PERMISSION_GRANTED);
        } catch (PackageManager.NameNotFoundException e) {
            text.append("\n  未找到权限定义：").append(name);
        } catch (RuntimeException e) {
            text.append("\n  权限检查失败：").append(describe(e));
        }
    }

    private static String protectionBase(int protection) {
        switch (protection) {
            case android.content.pm.PermissionInfo.PROTECTION_NORMAL: return "normal";
            case android.content.pm.PermissionInfo.PROTECTION_DANGEROUS: return "dangerous";
            case android.content.pm.PermissionInfo.PROTECTION_SIGNATURE: return "signature";
            default: return "other:" + protection;
        }
    }

    private void probeRewrite(int token) {
        Rewriter client = null;
        java.util.concurrent.Future<Integer> pending = null;
        try {
            client = Rewriting.getClient(RewriterOptions.builder(getApplicationContext())
                    .setLanguage(RewriterOptions.Language.ENGLISH)
                    .setOutputType(RewriterOptions.OutputType.PROFESSIONAL).build());
            pending = client.checkFeatureStatus();
            int status = pending.get(20, TimeUnit.SECONDS);
            publish(token, "英语正式语气改写：" + statusText(status));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception | LinkageError e) {
            publish(token, "英语正式语气改写：检测失败\n" + describe(e));
        } finally {
            if (pending != null && !pending.isDone()) pending.cancel(true);
            if (client != null) {
                try { client.close(); } catch (RuntimeException ignored) { }
            }
        }
    }

    private void probeProofread(int token) {
        Proofreader client = null;
        java.util.concurrent.Future<Integer> pending = null;
        try {
            client = Proofreading.getClient(ProofreaderOptions.builder(getApplicationContext())
                    .setLanguage(ProofreaderOptions.Language.ENGLISH)
                    .setInputType(ProofreaderOptions.InputType.KEYBOARD).build());
            pending = client.checkFeatureStatus();
            int status = pending.get(20, TimeUnit.SECONDS);
            publish(token, "英语键盘文本校对：" + statusText(status));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception | LinkageError e) {
            publish(token, "英语键盘文本校对：检测失败\n" + describe(e));
        } finally {
            if (pending != null && !pending.isDone()) pending.cancel(true);
            if (client != null) {
                try { client.close(); } catch (RuntimeException ignored) { }
            }
        }
    }

    private void publish(int token, String line) {
        runOnUiThread(() -> {
            if (active && token == generation && !isDestroyed()) report.append(line + "\n");
        });
    }

    private static String statusText(int status) {
        switch (status) {
            case FeatureStatus.UNAVAILABLE: return "UNAVAILABLE（当前配置不可用）";
            case FeatureStatus.DOWNLOADABLE: return "DOWNLOADABLE（模型可下载，尚未就绪）";
            case FeatureStatus.DOWNLOADING: return "DOWNLOADING（模型正在下载）";
            case FeatureStatus.AVAILABLE: return "AVAILABLE（功能已就绪）";
            default: return "UNKNOWN（" + status + "）";
        }
    }

    private static String describe(Throwable error) {
        StringBuilder text = new StringBuilder();
        Throwable current = error;
        for (int depth = 0; current != null && depth < 5; depth++) {
            if (depth != 0) text.append("\n原因：");
            text.append(current.getClass().getSimpleName()).append(": ")
                    .append(current.getMessage() == null ? "(无详细消息)" : current.getMessage());
            Throwable next = current.getCause();
            if (next == current) break;
            current = next;
        }
        return text.toString();
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        out.putString("report", report.getText().toString());
        super.onSaveInstanceState(out);
    }

    @Override protected void onStop() {
        active = false;
        generation++;
        if (job != null) job.cancel(true);
        if (busy) report.append("\n检测已暂停：请保持页面前台后重新检测。");
        busy = false;
        check.setEnabled(true);
        super.onStop();
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
