package dev.kaede.pixelboard;

import android.app.Activity;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** On-device test configuration. Changes require restarting the Gboard process. */
public final class SettingsActivity extends Activity {
    private SharedPreferences prefs;
    private SharedPreferences runtimePrefs;
    private TextView runtimeReport;
    private final SharedPreferences.OnSharedPreferenceChangeListener runtimeListener =
            (preferences, key) -> runOnUiThread(this::refreshRuntimeStatus);
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("features", MODE_PRIVATE);
        ScrollView root = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        int pad = (int)(20 * getResources().getDisplayMetrics().density);
        list.setPadding(pad,pad,pad,pad);
        root.addView(list);
        addText(list, "Gboard Rambler · API 102", true);
        addText(list, "Experimental features. Changes take effect after Gboard is stopped and restarted. Not all features are implemented.", false);
        addToggle(list,"rambler","Rambler (experimental)","当前设备已验证 Rambler；更改后需重启 Gboard。");
        addToggle(list,"spellchecker","Inline spellchecker (experimental)","Obfuscated method mapping needs on-device validation.");
        addToggle(list,"diagnostics","Diagnostic hooks","Observe selection and emit Android logcat messages.");
        addToggle(list,"writing_tools","AI 写作工具 · 总开关","默认使用基础云端校对/改写；V2 需另行开启。0.1.7 已验证入口及英语校对；修改后需强制停止并重新打开 Gboard。建议先在便签中使用英语测试，默认校对应用排除列表包含 ChatGPT。");
        addToggle(list, "writing_tools_v2", "Writing Tools V2 · 实验模式",
                "需同时开启 AI 写作工具。仅适配 Gboard 18.4.1.985164140（176027078）。"
                + "可能触发官方模型准备或下载，需要完成 Gboard 自身的使用同意。"
                + "缺少服务绑定权限时自动保留基础云端模式；其他不可用情况可关闭本开关并重启 Gboard 恢复。");
        addToggle(list, "v2_local_proofread", "AICore 端侧校对 · 可选实验",
                "默认关闭。AICore 私有改写与端侧校对分别检查可用性；开启后会额外等待端侧校对。"
                + "本轮请保持关闭，仅测试私有改写路径。");
        addToggle(list, "gboard_pixel_identity", "仅 Gboard 伪装 Pixel 9 Pro · V2 实验",
                "默认关闭，需同时开启 AI 写作工具和 V2。仅在 Gboard 进程内修改机型字段，"
                + "用于比较本地设备判断；更改后必须重启 Gboard。"
                + "AICore/PCS、系统属性和硬件证明保持原状；不保证获得模型或通过服务器认证。"
                + "本实验会影响 Gboard 进程内其他读取这些字段的功能。关闭后重启 Gboard 恢复。");
        addText(list, "V2 后端（仅实验模式生效）", true);
        android.widget.RadioGroup backends = new android.widget.RadioGroup(this);
        android.widget.RadioButton aicore = new android.widget.RadioButton(this);
        aicore.setId(View.generateViewId());
        aicore.setText("AICore 私有推理（可能结合端侧模型）");
        android.widget.RadioButton astrea = new android.widget.RadioButton(this);
        astrea.setId(View.generateViewId());
        astrea.setText("Astrea 私有推理（需要联网）");
        backends.addView(aicore);
        backends.addView(astrea);
        backends.check(prefs.getInt("writing_tools_backend", 2) == 3 ? astrea.getId() : aicore.getId());
        TextView prerequisites = new TextView(this);
        prerequisites.setText(WritingToolsBackendCheck.check(this,
                prefs.getInt("writing_tools_backend", 2)).message);
        backends.setOnCheckedChangeListener((group, id) -> {
            prefs.edit().putInt("writing_tools_backend", id == astrea.getId() ? 3 : 2).apply();
            ((ModuleApp) getApplication()).sync();
            prerequisites.setText(WritingToolsBackendCheck.check(this,
                    prefs.getInt("writing_tools_backend", 2)).message);
        });
        list.addView(backends);
        list.addView(prerequisites);
        addText(list, "实验作用域：Gboard、AICore、Private Compute Services。"
                + "V2 与下方模型下载准备分别控制。选择后端只改变请求路径，不代表模型、账号或服务已可用。", false);
        addToggle(list, "local_model_preparation", "AICore 模型下载准备 · 独立实验",
                "独立于 Gboard V2，供下方基础模型测试使用。开启 PCS 受保护下载配置，"
                + "不强制模型可用；模型清单仍可能被拒绝。修改后点击重载目标进程。"
                + "需将 Private Compute Services 纳入 LSPosed 作用域；下载可能产生流量。");
        android.widget.Button local = new android.widget.Button(this);
        local.setText("AICore 端侧执行 · 基础模型");
        local.setOnClickListener(v -> startActivity(new android.content.Intent(this, AiCoreLocalActivity.class)));
        list.addView(local);
        android.widget.Button status = new android.widget.Button(this);
        status.setText("后端信息与公开 API 检测");
        status.setOnClickListener(v -> startActivity(new android.content.Intent(this, AiCoreProbeActivity.class)));
        list.addView(status);
        addText(list, "LSPosed framework service: " + (((ModuleApp)getApplication()).connected() ? "connected" : "not connected") + ". Configuration sync is retried upon connection.", false);
        addText(list, "Logcat filter: PixelBoardLS. Toggle state is not proof of successful Hook installation.", false);
        addText(list, "模块运行状态", true);
        addText(list, "显示本模块收到的启动与请求事件，不包含输入文本。"
                + "通道自检只验证状态传递，不代表已注入 Gboard。", false);
        runtimePrefs = getSharedPreferences(RuntimeStatus.PREFS, MODE_PRIVATE);
        runtimeReport = new TextView(this);
        runtimeReport.setTextIsSelectable(true);
        list.addView(runtimeReport);
        android.widget.Button copyStatus = new android.widget.Button(this);
        copyStatus.setText("复制运行状态");
        copyStatus.setOnClickListener(v -> {
            refreshRuntimeStatus();
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                    getSystemService(CLIPBOARD_SERVICE);
            if (clipboard != null) clipboard.setPrimaryClip(android.content.ClipData.newPlainText(
                    "Writing Tools 运行状态", runtimeReport.getText()));
            android.widget.Toast.makeText(this, "已复制", android.widget.Toast.LENGTH_SHORT).show();
        });
        list.addView(copyStatus);
        android.widget.Button channelTest = new android.widget.Button(this);
        channelTest.setText("诊断通道自检");
        channelTest.setOnClickListener(v -> RuntimeStatus.send(this, "CHANNEL_TEST", "transport_only=true"));
        list.addView(channelTest);
        android.widget.Button live = new android.widget.Button(this);
        live.setText("查询 Gboard 与 PCS（实时应答）");
        live.setOnClickListener(v -> {
            try { RuntimeProbe.request(this); }
            catch (RuntimeException e) {
                runtimePrefs.edit().putString("probe_reply",
                        "实时查询发送失败：" + e.getClass().getSimpleName()).apply();
            }
            refreshRuntimeStatus();
        });
        list.addView(live);
        addText(list, "可选 Root 重载会停止 Gboard、AICore 和 Private Compute Services。"
                + "仅点击按钮时请求 Root；随后点击下方输入框唤起键盘，再查询实时应答。", false);
        android.widget.Button reload = new android.widget.Button(this);
        reload.setText("重载目标进程（Root）");
        reload.setOnClickListener(v -> {
            reload.setEnabled(false);
            runtimePrefs.edit().putString("reload_result", "正在请求 Root 并停止目标进程…")
                    .remove("probe_client_version").remove("probe_nonce")
                    .remove("probe_reply").remove("probe_time").remove("pcs_probe_reply").apply();
            android.content.Context app = getApplicationContext();
            new Thread(() -> {
                String result = RuntimeReload.run();
                app.getSharedPreferences(RuntimeStatus.PREFS, MODE_PRIVATE).edit()
                        .putString("reload_result", result).apply();
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) reload.setEnabled(true);
                });
            }, "PixelBoard-reload").start();
        });
        list.addView(reload);
        addText(list, "测试输入框：可在此唤起 Gboard，再尝试写作工具。", false);
        android.widget.EditText editor = new android.widget.EditText(this);
        editor.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                | android.text.InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);
        editor.setText("I has a meeting tomorrow.");
        list.addView(editor);
        setContentView(root);
    }

    @Override protected void onResume() {
        super.onResume();
        runtimePrefs.registerOnSharedPreferenceChangeListener(runtimeListener);
        refreshRuntimeStatus();
    }

    @Override protected void onPause() {
        runtimePrefs.unregisterOnSharedPreferenceChangeListener(runtimeListener);
        super.onPause();
    }

    private void refreshRuntimeStatus() {
        if (runtimeReport == null || runtimePrefs == null) return;
        String version = BuildConfig.VERSION_NAME;
        String history = runtimePrefs.getString(RuntimeStatusFormat.historyKey(version), "");
        String attached = runtimePrefs.getString("last_v_" + version + "_"
                + RuntimeStatus.GBOARD + "_GBOARD_ATTACHED", "");
        boolean requested = version.equals(runtimePrefs.getString("probe_client_version", ""))
                && runtimePrefs.getLong("probe_time", 0) > 0;
        String reply = requested ? runtimePrefs.getString("probe_reply", "") : "";
        runtimeReport.setText("模块版本：" + version
                + "\n保存配置：V2=" + (prefs.getBoolean("writing_tools", false)
                        && prefs.getBoolean("writing_tools_v2", false))
                + " 后端=" + prefs.getInt("writing_tools_backend", 2)
                + " 端侧校对请求=" + prefs.getBoolean("v2_local_proofread", false)
                + " Pixel伪装请求=" + prefs.getBoolean("gboard_pixel_identity", false)
                + "\n本版本 Gboard 机型事件（历史记录，实际读回以实时应答为准）：\n"
                + runtimePrefs.getString("last_v_" + version + "_" + RuntimeStatus.GBOARD
                        + "_GBOARD_IDENTITY", "尚未收到；保存开关不代表已经生效。")
                + "\n本次实时查询：\n" + (!reply.isEmpty() ? reply
                        : requested ? "尚未收到本次应答；不能据此判定未注入。"
                        : "尚未查询。请唤起 Gboard 后点击实时查询。")
                + (prefs.getInt("writing_tools_backend", 2) == 3
                        ? "\nAstrea READY 仅为前置开关检查；请以实际请求的 V2_RESPONSE / V2_ASTREA_ERROR 为准。" : "")
                + "\n本次 PCS 主动查询：\n" + (requested
                        ? runtimePrefs.getString("pcs_probe_reply", "").isEmpty()
                                ? "等待本次 PCS 查询结果；请保持模块界面在前台。"
                                : runtimePrefs.getString("pcs_probe_reply", "")
                        : "尚未查询。先执行一次写作工具请求，再点击实时查询。")
                + "\n当前版本启动事件：\n" + (attached.isEmpty() ? "尚未收到。" : attached)
                + "\n本版本最近 PCS 事件（按事件时间与 PID 判断）：\n" + backendEvents(version)
                + "\nRoot 重载结果：\n" + runtimePrefs.getString("reload_result", "尚未执行。")
                + "\n当前模块版本最近 40 条事件：\n"
                + (history.isEmpty() ? "暂无。旧版历史不作为当前状态显示。" : history));
    }


    private String backendEvents(String version) {
        String prefix = "last_v_" + version + "_com.google.android.as.oss_";
        StringBuilder out = new StringBuilder();
        for (String event : new String[] {"BACKEND_PACKAGE", "INIT_FAILED", "MODE",
                "V2_DIAGNOSTICS_READY", "V2_DIAGNOSTICS_FAILED", "V2_PROFILE_FAILED",
                "ASTREA_FLAG", "ASTREA_ATTESTATION"}) {
            String line = runtimePrefs.getString(prefix + event, "");
            if (!line.isEmpty()) out.append(line).append("\n");
        }
        return out.length() == 0 ? "后台事件通道尚未收到 PCS 事件；请参考上方主动查询或 LSPosed 日志，不能据此判定观察器未生效。"
                : out.toString();
    }

    private void addToggle(LinearLayout parent, String key, String title, String note) {
        CheckBox toggle = new CheckBox(this);
        toggle.setText(title);
        toggle.setChecked(prefs.getBoolean(key,false));
        toggle.setOnCheckedChangeListener((button, checked) ->
            { prefs.edit().putBoolean(key,checked).apply();
                ((ModuleApp)getApplication()).sync(); });
        parent.addView(toggle);
        addText(parent,note,false);
    }
    private void addText(LinearLayout parent, String s, boolean heading) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(heading?22:14);
        if (heading) t.setTypeface(null, Typeface.BOLD);
        t.setPadding(0,0,0,(int)(14*getResources().getDisplayMetrics().density));
        parent.addView(t);
    }
}


