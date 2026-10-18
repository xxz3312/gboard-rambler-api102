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
                + "关闭 V2 时，不修改 AICore/Astrea。选择后端只改变请求路径，不代表模型、账号或服务已可用。", false);
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
        String history = runtimePrefs.getString("history", "");
        String attached = runtimePrefs.getString(
                "last_com.google.android.inputmethod.latin_GBOARD_ATTACHED", "");
        String activity = "";
        for (java.util.Map.Entry<String, ?> entry : runtimePrefs.getAll().entrySet())
            if (entry.getKey().startsWith("last_com.google.android.inputmethod.latin_")
                    && entry.getValue() instanceof String
                    && ((String) entry.getValue()).compareTo(activity) > 0)
                activity = (String) entry.getValue();
        String initialization = !attached.isEmpty() ? attached
                : !activity.isEmpty() ? "已收到 Gboard 运行事件；启动事件未保存。\n" + activity
                : "尚未收到 Gboard 事件。可能尚未重启进程或状态通道未送达。";
        runtimeReport.setText("模块版本：" + BuildConfig.VERSION_NAME
                + "\n保存配置：V2=" + (prefs.getBoolean("writing_tools", false)
                        && prefs.getBoolean("writing_tools_v2", false))
                + " 后端=" + prefs.getInt("writing_tools_backend", 2)
                + " 端侧校对请求=" + prefs.getBoolean("v2_local_proofread", false)
                + "\n最近一次 Gboard 初始化事件：\n"
                + initialization
                + "\n最近 40 条事件：\n"
                + (history.isEmpty() ? "暂无事件。可先点击诊断通道自检。" : history));
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
