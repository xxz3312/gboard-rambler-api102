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
        title.setText("AICore 本地写作能力检测");
        title.setTextSize(22);
        list.addView(title);
        TextView explanation = new TextView(this);
        explanation.setText("检测英语校对和英语正式语气改写的官方 API 状态。"
                + "请保持此页在前台。检测不会主动下载模型或提交推理请求，也不会切换 Gboard 后端。"
                + "\n结果仅代表本模块调用公开 API 的能力，不代表 Gboard 私有后端或 Astrea 可用。");
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
            report.setText("点击开始检测。");
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
                        + "\nAstrea：本检测未验证。Gboard：仍使用原来的云端配置。");
                busy = false;
                check.setEnabled(true);
            });
        });
    }

    private String deviceReport() {
        StringBuilder text = new StringBuilder("版本：0.1.3-aicore-bind\n");
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
        appendBindingReport(text);
        text.append("\n注意：官方文档不支持解锁 Bootloader 的设备；"
                + "服务初始化、机型、语言和模型配置也会影响结果。\n");
        return text.toString();
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
