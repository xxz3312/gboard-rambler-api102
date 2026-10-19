package dev.kaede.pixelboard;

import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textview.MaterialTextView;

/** Module UI; launcher visibility never changes the exported settings component. */
public final class SettingsActivity extends AppCompatActivity {
    private SharedPreferences prefs;
    private MaterialSwitch launcherSwitch;
    private boolean refreshingLauncher;
    private Runnable statusRefresh;
    private final android.os.Handler statusHandler=new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable statusTicker=new Runnable() {
        @Override public void run() {
            if(statusRefresh!=null)statusRefresh.run();
            statusHandler.postDelayed(this,1000);
        }
    };
    @Override protected void onResume() {
        super.onResume(); statusHandler.removeCallbacks(statusTicker); statusHandler.post(statusTicker);
    }
    @Override protected void onPause() {
        statusHandler.removeCallbacks(statusTicker); super.onPause();
    }
    @Override protected void onCreate(Bundle state) {
        DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(state);
        prefs = getSharedPreferences("features", MODE_PRIVATE);
        ScrollView root = new ScrollView(this);
        root.setFillViewport(true);
        LinearLayout list = column();
        list.setPadding(dp(20), dp(24), dp(20), dp(32));
        root.addView(list);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        text(list, "Gboard Rambler", com.google.android.material.R.style.TextAppearance_Material3_HeadlineLarge);
        text(list, "API 102 · " + CloudRuntimeStatus.VERSION, com.google.android.material.R.style.TextAppearance_Material3_LabelLarge);
        info(list, "写作工具", "使用基础云端校对与改写。修改开关后需重启 Gboard；版本适配和 Hook 状态见下方。");
        section(list, "功能");
        toggle(list, "rambler", "Rambler", "实验功能；原版路径已在用户设备验证。", false);
        toggle(list, "writing_tools", "AI 写作工具", "基础云端校对与改写。需联网并完成 Gboard 同意流程。", false);
        section(list, "诊断");
        toggle(list, "diagnostics", "诊断日志", "记录 Hook 状态。Logcat 标签：PixelBoardLS。", false);
        section(list, "外观");
        launcherSwitch = toggle(list, null, "隐藏桌面图标",
                "隐藏后可从 LSPosed 模块页打开设置，关闭此开关即可恢复。", isLauncherHidden());
        launcherSwitch.setOnCheckedChangeListener((button, hidden) -> {
            if (refreshingLauncher) return;
            if (hidden) {
                new MaterialAlertDialogBuilder(this)
                    .setTitle("隐藏桌面图标？")
                    .setMessage("请确认你能从 LSPosed 的模块页面打开设置。隐藏只影响桌面入口，模块仍会运行。也可通过 ADB 恢复，命令见 README。")
                    .setNegativeButton("取消", (dialog, which) -> refreshLauncher())
                    .setPositiveButton("隐藏", (dialog, which) -> setLauncherHidden(true))
                    .setOnCancelListener(dialog -> refreshLauncher()).show();
            } else setLauncherHidden(false);
        });
        section(list, "状态与来源");
        TextView runtime = text(list, "", com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        Runnable refreshStatus = () -> {
            runtime.setText(((ModuleApp) getApplication()).configurationStatus()
                    + "\n\n" + CloudRuntimeStatus.report(this));
            runtime.setTextColor(CloudRuntimeStatus.unsupported(this)
                    ? com.google.android.material.color.MaterialColors.getColor(runtime,
                        androidx.appcompat.R.attr.colorError)
                    : com.google.android.material.color.MaterialColors.getColor(runtime,
                        com.google.android.material.R.attr.colorOnSurface));
        };
        statusRefresh=refreshStatus;
        refreshStatus.run();
        MaterialButton refresh = new MaterialButton(this);
        refresh.setText("同步配置并刷新运行状态");
        refresh.setOnClickListener(v -> {
            ((ModuleApp) getApplication()).sync();
            refreshStatus.run();
        });
        list.addView(refresh);
        MaterialButton restart = new MaterialButton(this);
        restart.setText("Root 重启 Gboard");
        restart.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
            .setTitle("重启 Gboard？")
            .setMessage("将强制停止 Gboard；当前正在输入的未提交内容可能丢失。随后请手动唤起键盘。")
            .setNegativeButton("取消", null)
            .setPositiveButton("重启", (dialog, which) -> {
                ((ModuleApp) getApplication()).sync();
                restart.setEnabled(false);
                new Thread(() -> {
                    String result;
                    try {
                        java.lang.Process process = new ProcessBuilder("su", "-c",
                                "am force-stop com.google.android.inputmethod.latin").start();
                        boolean done = process.waitFor(15, java.util.concurrent.TimeUnit.SECONDS);
                        if (!done) { process.destroy(); result = "Root 请求超时；可从系统设置强制停止 Gboard。"; }
                        else result = process.exitValue() == 0
                                ? "已发送停止请求，请唤起 Gboard 后刷新状态。"
                                : "Root 停止失败；可从系统设置强制停止 Gboard。";
                    } catch (Exception error) { result = "无法执行 Root 停止：" + error.getClass().getSimpleName(); }
                    String message = result;
                    runOnUiThread(() -> {
                        restart.setEnabled(true);
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    });
                }, "gboard-restart").start();
            }).show());
        list.addView(restart);
        info(list, "使用范围", "云端服务受网络、语言、账号及服务端策略影响。建议在便签中使用英语测试。ChatGPT 等应用可能被 Gboard 默认排除。开关开启不代表推理成功。");
        info(list, "开源许可", "维护：Kaede。基于 Akshayykadam/PixelBoard；GPL-3.0。完整许可与署名随 APK assets/licenses 提供。非 Google 官方组件。");
        setContentView(root);
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private LinearLayout column() {
        LinearLayout result = new LinearLayout(this);
        result.setOrientation(LinearLayout.VERTICAL);
        return result;
    }
    private TextView text(LinearLayout parent, String value, int appearance) {
        MaterialTextView view = new MaterialTextView(this);
        view.setTextAppearance(appearance);
        view.setText(value);
        view.setPadding(0, dp(6), 0, dp(8));
        parent.addView(view);
        return view;
    }
    private void section(LinearLayout parent, String title) {
        TextView label = text(parent, title, com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
        label.setPadding(0, dp(24), 0, dp(12));
    }
    private LinearLayout card(LinearLayout parent) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(24));
        card.setCardElevation(0);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(10);
        parent.addView(card, params);
        LinearLayout contents = column();
        contents.setPadding(dp(20), dp(12), dp(20), dp(16));
        card.addView(contents);
        return contents;
    }
    private void info(LinearLayout parent, String title, String body) {
        LinearLayout contents = card(parent);
        text(contents, title, com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
        text(contents, body, com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
    }
    private MaterialSwitch toggle(LinearLayout parent, String key, String title, String note, boolean initial) {
        LinearLayout contents = card(parent);
        MaterialSwitch toggle = new MaterialSwitch(this);
        toggle.setText(title);
        toggle.setMinHeight(dp(56));
        toggle.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
        toggle.setChecked(key == null ? initial : prefs.getBoolean(key, false));
        contents.addView(toggle, new LinearLayout.LayoutParams(-1, -2));
        text(contents, note, com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        if (key != null) toggle.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean(key, checked).apply();
            ((ModuleApp) getApplication()).sync();
        });
        return toggle;
    }
    private ComponentName launcher() { return new ComponentName(this, getPackageName() + ".Launcher"); }
    private boolean isLauncherHidden() {
        return getPackageManager().getComponentEnabledSetting(launcher()) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
    }
    private void refreshLauncher() {
        refreshingLauncher = true;
        launcherSwitch.setChecked(isLauncherHidden());
        refreshingLauncher = false;
    }
    private void setLauncherHidden(boolean hidden) {
        try {
            getPackageManager().setComponentEnabledSetting(launcher(),
                    hidden ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED : PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
            Toast.makeText(this, hidden ? "桌面图标已隐藏；可从 LSPosed 打开设置" : "桌面图标已恢复，桌面可能稍后刷新", Toast.LENGTH_LONG).show();
        } catch (RuntimeException error) {
            Toast.makeText(this, "无法修改桌面图标：" + error.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
        }
        refreshLauncher();
    }
}

