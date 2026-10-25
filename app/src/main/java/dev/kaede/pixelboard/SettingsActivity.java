package dev.kaede.pixelboard;

import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.TextAppearanceSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.divider.MaterialDivider;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textview.MaterialTextView;

/** Compact Material 3 settings. Launcher visibility never disables module settings. */
public final class SettingsActivity extends AppCompatActivity {
    private SharedPreferences prefs;
    private MaterialSwitch launcherSwitch;
    private boolean refreshingLauncher;
    private LinearLayout root;
    private MaterialTextView frameworkSummary, compatibilityTitle, compatibilitySummary;
    private View restartAction;
    private TextView detailText;
    private final android.os.Handler statusHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable statusTicker = new Runnable() {
        @Override public void run() {
            refreshStatus();
            statusHandler.postDelayed(this, 1000);
        }
    };

    @Override protected void onResume() {
        super.onResume();
        if (launcherSwitch != null) refreshLauncher();
        statusHandler.removeCallbacks(statusTicker);
        statusHandler.post(statusTicker);
    }

    @Override protected void onPause() {
        statusHandler.removeCallbacks(statusTicker);
        super.onPause();
    }

    @Override protected void onCreate(Bundle state) {
        DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(state);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        prefs = getSharedPreferences("features", MODE_PRIVATE);
        root = column();
        int surface = color(com.google.android.material.R.attr.colorSurface);
        root.setBackgroundColor(surface);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        boolean light = ColorUtils.calculateLuminance(surface) > 0.5;
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        MaterialToolbar toolbar = new MaterialToolbar(this);
        toolbar.setTitle("Gboard Rambler");
        toolbar.setTitleTextAppearance(this, com.google.android.material.R.style.TextAppearance_Material3_TitleLarge);
        toolbar.setBackgroundColor(surface);
        root.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(64)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        FrameLayout frame = new FrameLayout(this);
        scroll.addView(frame);
        LinearLayout list = column();
        list.setPadding(dp(16), 0, dp(16), dp(24));
        // Keep settings readable in landscape and on large displays.
        int width = getResources().getConfiguration().screenWidthDp > 640 ? dp(640) : -1;
        frame.addView(list, new FrameLayout.LayoutParams(width, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL));

        section(list, "功能");
        LinearLayout features = group(list);
        toggle(features, "rambler", "Rambler", "语音输入与智能编辑", R.drawable.ic_mic, false);
        divider(features);
        toggle(features, "writing_tools", "AI 写作工具", "联网校对与改写", R.drawable.ic_edit, false);
        divider(features);
        toggle(features, "scan_text", "扫描文本 / 相机 OCR", "启用 Gboard 工具栏中的扫描文本", R.drawable.ic_edit, false);
        divider(features);
        toggle(features, "screenshot_suggestions", "截图浮动建议", "在 Gboard 中显示最近截图与图片建议", R.drawable.ic_info, false);
        MaterialTextView hint = label("更改功能后，请重新启动 Gboard。",
                com.google.android.material.R.style.TextAppearance_Material3_BodySmall,
                com.google.android.material.R.attr.colorOnSurfaceVariant);
        hint.setPadding(dp(16), dp(8), dp(16), 0);
        list.addView(hint);

        section(list, "外观");
        LinearLayout appearance = group(list);
        toggle(appearance, "rounded_keys", "Material 3 表现力圆角键", "启用 Belka UI 胶囊形按键", R.drawable.ic_edit, false);
        divider(appearance);
        launcherSwitch = toggle(appearance, null, "隐藏桌面图标", "隐藏后可从 LSPosed 打开设置",
                R.drawable.ic_visibility_off, isLauncherHidden());
        launcherSwitch.setOnCheckedChangeListener((button, hidden) -> {
            if (refreshingLauncher) return;
            if (hidden) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("隐藏桌面图标？")
                        .setMessage("隐藏后可从 LSPosed 模块页打开设置，并关闭此开关恢复图标。")
                        .setNegativeButton("取消", (dialog, which) -> refreshLauncher())
                        .setPositiveButton("隐藏", (dialog, which) -> setLauncherHidden(true))
                        .setOnCancelListener(dialog -> refreshLauncher()).show();
            } else setLauncherHidden(false);
        });

        section(list, "运行与诊断");
        LinearLayout status = group(list);
        row(status, "相机与图片权限", "查看 Gboard 权限及使用条件", R.drawable.ic_info, () ->
                new MaterialAlertDialogBuilder(this).setTitle("Gboard 相机与图片权限")
                        .setMessage(HostFeaturePermissions.summary(this)+"\n\n请在 Gboard 的权限页面授权。OCR 从工具栏的扫描文本进入；截图建议还需在 Gboard 剪贴板设置中启用。\n\n若官方安装包未声明相应权限，LSPosed 功能开关无法添加权限，此安装包可能无法使用对应功能。组件缺失或版本差异也可能影响入口。")
                        .setNegativeButton("关闭", null)
                        .setPositiveButton("Gboard 设置", (dialog, which) -> {
                            try { startActivity(new android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    android.net.Uri.parse("package:com.google.android.inputmethod.latin"))); }
                            catch (RuntimeException error) { Toast.makeText(this,"无法打开应用设置",Toast.LENGTH_SHORT).show(); }
                        }).show());
        divider(status);
        Row framework = row(status, "框架服务", "", R.drawable.ic_link, null);
        frameworkSummary = framework.summary;
        divider(status);
        Row compatibility = row(status, "适配与运行状态", "查看 Gboard 适配和 Hook 详情",
                R.drawable.ic_info, this::showRuntimeDetails);
        compatibilityTitle = compatibility.title;
        compatibilitySummary = compatibility.summary;
        divider(status);
        row(status, "同步配置", "将开关设置同步到 LSPosed", R.drawable.ic_sync, () -> {
            ((ModuleApp) getApplication()).sync();
            refreshStatus();
            Snackbar.make(root, ModuleApp.connected() ? "已请求同步，结果见运行详情" : "等待 LSPosed 服务连接",
                    Snackbar.LENGTH_SHORT).show();
        });
        divider(status);
        restartAction = row(status, "重启 Gboard", "需要 Root 权限", R.drawable.ic_restart,
                this::confirmRestart).view;
        divider(status);
        toggle(status, "diagnostics", "诊断日志", "记录适配与 Hook 状态", R.drawable.ic_diagnostics, false);

        section(list, "关于");
        LinearLayout about = group(list);
        row(about, "使用说明", "网络、语言与使用范围", R.drawable.ic_help, () ->
                new MaterialAlertDialogBuilder(this).setTitle("使用说明")
                        .setMessage("AI 写作工具需要联网，并完成 Gboard 的同意流程。建议先在普通便签中用英语测试；部分应用可能被 Gboard 排除。\n\n开关开启、配置同步或适配成功不代表云端请求成功。修改功能后，请强制停止并重新唤起 Gboard。")
                        .setPositiveButton("知道了", null).show());
        divider(about);
        row(about, "Gboard Rambler", "版本 " + CloudRuntimeStatus.VERSION + " · Kaede", R.drawable.ic_info, () ->
                new MaterialAlertDialogBuilder(this).setTitle("关于 Gboard Rambler")
                        .setMessage("版本 " + CloudRuntimeStatus.VERSION + " · API 102\n维护：Kaede\n\n基于 Akshayykadam/PixelBoard，采用 GPL-3.0。完整许可与署名随 APK 的 assets/licenses 提供。本项目非 Google 官方组件。")
                        .setPositiveButton("关闭", null).show());
        setContentView(root);
        // PhoneWindow creates its DecorView during setContentView; do not query its controller earlier.
        androidx.core.view.WindowInsetsControllerCompat systemBars =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        systemBars.setAppearanceLightStatusBars(light);
        systemBars.setAppearanceLightNavigationBars(light);
        ViewCompat.requestApplyInsets(root);
        refreshStatus();
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private int color(int attr) { return MaterialColors.getColor(this, attr, "SettingsActivity"); }
    private LinearLayout column() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        return view;
    }
    private MaterialTextView label(String value, int appearance, int colorAttr) {
        MaterialTextView view = new MaterialTextView(this);
        view.setTextAppearance(appearance);
        view.setTextColor(color(colorAttr));
        view.setText(value);
        return view;
    }
    private void section(LinearLayout list, String title) {
        MaterialTextView label = label(title, com.google.android.material.R.style.TextAppearance_Material3_LabelLarge,
                androidx.appcompat.R.attr.colorPrimary);
        label.setAccessibilityHeading(true);
        label.setPadding(dp(16), dp(24), dp(16), dp(8));
        list.addView(label);
    }
    private LinearLayout group(LinearLayout list) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(16));
        card.setCardElevation(0);
        card.setStrokeWidth(0);
        card.setClipToOutline(true);
        card.setCardBackgroundColor(color(com.google.android.material.R.attr.colorSurfaceContainerLow));
        list.addView(card, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout contents = column();
        card.addView(contents);
        return contents;
    }
    private void divider(LinearLayout group) {
        MaterialDivider divider = new MaterialDivider(this);
        divider.setDividerColor(color(com.google.android.material.R.attr.colorOutlineVariant));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(1));
        params.setMarginStart(dp(56));
        params.setMarginEnd(dp(16));
        group.addView(divider, params);
    }
    private Drawable icon(int resource) {
        Drawable icon = AppCompatResources.getDrawable(this, resource).mutate();
        icon.setTint(color(com.google.android.material.R.attr.colorOnSurfaceVariant));
        icon.setBounds(0, 0, dp(24), dp(24));
        return icon;
    }
    private MaterialSwitch toggle(LinearLayout group, String key, String title, String note,
            int resource, boolean initial) {
        MaterialSwitch toggle = new MaterialSwitch(this);
        SpannableString text = new SpannableString(title + "\n" + note);
        text.setSpan(new TextAppearanceSpan(this, com.google.android.material.R.style.TextAppearance_Material3_BodyLarge),
                0, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.setSpan(new TextAppearanceSpan(this, com.google.android.material.R.style.TextAppearance_Material3_BodyMedium),
                title.length() + 1, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.setSpan(new ForegroundColorSpan(color(com.google.android.material.R.attr.colorOnSurfaceVariant)),
                title.length() + 1, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        toggle.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);
        toggle.setTextColor(color(com.google.android.material.R.attr.colorOnSurface));
        toggle.setText(text);
        toggle.setGravity(Gravity.CENTER_VERTICAL);
        toggle.setMinHeight(dp(72));
        toggle.setPaddingRelative(dp(16), dp(12), dp(16), dp(12));
        toggle.setCompoundDrawablesRelative(icon(resource), null, null, null);
        toggle.setCompoundDrawablePadding(dp(16));
        toggle.setSwitchPadding(dp(16));
        toggle.setChecked(key == null ? initial : prefs.getBoolean(key, false));
        group.addView(toggle, new LinearLayout.LayoutParams(-1, -2));
        if (key != null) toggle.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean(key, checked).apply();
            ((ModuleApp) getApplication()).sync();
            if (!"diagnostics".equals(key))
                Snackbar.make(root, "重新启动 Gboard 后生效", Snackbar.LENGTH_SHORT).show();
        });
        return toggle;
    }
    private static final class Row {
        final LinearLayout view;
        final MaterialTextView title, summary;
        Row(LinearLayout view, MaterialTextView title, MaterialTextView summary) {
            this.view = view; this.title = title; this.summary = summary;
        }
    }
    private Row row(LinearLayout group, String title, String summary, int resource, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(72));
        row.setPaddingRelative(dp(16), dp(12), dp(16), dp(12));
        ImageView image = new ImageView(this);
        image.setImageDrawable(icon(resource));
        image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(dp(24), dp(24));
        imageParams.setMarginEnd(dp(16));
        row.addView(image, imageParams);
        LinearLayout labels = column();
        MaterialTextView headline = label(title, com.google.android.material.R.style.TextAppearance_Material3_BodyLarge,
                com.google.android.material.R.attr.colorOnSurface);
        MaterialTextView supporting = label(summary, com.google.android.material.R.style.TextAppearance_Material3_BodyMedium,
                com.google.android.material.R.attr.colorOnSurfaceVariant);
        labels.addView(headline);
        labels.addView(supporting);
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        if (action != null) {
            ImageView chevron = new ImageView(this);
            chevron.setImageDrawable(icon(R.drawable.ic_chevron));
            chevron.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            LinearLayout.LayoutParams trailing = new LinearLayout.LayoutParams(dp(24), dp(24));
            trailing.setMarginStart(dp(16));
            row.addView(chevron, trailing);
            TypedValue background = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.selectableItemBackground, background, true);
            row.setBackgroundResource(background.resourceId);
            row.setFocusable(true);
            row.setOnClickListener(v -> action.run());
        }
        group.addView(row, new LinearLayout.LayoutParams(-1, -2));
        return new Row(row, headline, supporting);
    }
    private void refreshStatus() {
        if (frameworkSummary == null) return;
        setTextIfChanged(frameworkSummary, ModuleApp.connected() ? "已连接 LSPosed" : "等待 LSPosed 服务连接");
        boolean error = CloudRuntimeStatus.unsupported(this);
        String report = CloudRuntimeStatus.report(this);
        setTextIfChanged(compatibilityTitle, error
                ? (report.contains("不支持该版本 Gboard") ? "不支持该版本 Gboard" : "适配或初始化异常")
                : "适配与运行状态");
        compatibilityTitle.setTextColor(color(error ? androidx.appcompat.R.attr.colorError
                : com.google.android.material.R.attr.colorOnSurface));
        setTextIfChanged(compatibilitySummary, error ? "点按查看详细状态" : "查看 Gboard 适配和 Hook 详情");
        if (detailText != null) setTextIfChanged(detailText,
                ((ModuleApp) getApplication()).configurationStatus() + "\n\n" + report);
    }
    private void setTextIfChanged(TextView view, String text) {
        if (!text.contentEquals(view.getText())) view.setText(text);
    }
    private void showRuntimeDetails() {
        ScrollView scroll = new ScrollView(this);
        MaterialTextView text = label("", com.google.android.material.R.style.TextAppearance_Material3_BodyMedium,
                com.google.android.material.R.attr.colorOnSurface);
        text.setTextIsSelectable(true);
        text.setPadding(dp(24), dp(8), dp(24), dp(16));
        scroll.addView(text);
        detailText = text;
        refreshStatus();
        new MaterialAlertDialogBuilder(this).setTitle("适配与运行状态")
                .setView(scroll).setPositiveButton("关闭", null)
                .setOnDismissListener(dialog -> detailText = null).show();
    }
    private void confirmRestart() {
        new MaterialAlertDialogBuilder(this).setTitle("重启 Gboard？")
                .setMessage("将强制停止 Gboard，未提交的输入内容可能丢失。随后请手动唤起键盘。")
                .setNegativeButton("取消", null)
                .setPositiveButton("重启", (dialog, which) -> {
                    ((ModuleApp) getApplication()).sync();
                    restartAction.setEnabled(false);
                    new Thread(() -> {
                        String result;
                        try {
                            java.lang.Process process = new ProcessBuilder("su", "-c",
                                    "am force-stop com.google.android.inputmethod.latin").start();
                            boolean done = process.waitFor(15, java.util.concurrent.TimeUnit.SECONDS);
                            if (!done) { process.destroy(); result = "Root 请求超时，可从系统设置强制停止 Gboard。"; }
                            else result = process.exitValue() == 0 ? "请重新唤起 Gboard。" : "Root 停止失败。";
                        } catch (Exception error) { result = "无法执行 Root 停止：" + error.getClass().getSimpleName(); }
                        String message = result;
                        runOnUiThread(() -> {
                            if (isDestroyed()) return;
                            restartAction.setEnabled(true);
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                        });
                    }, "gboard-restart").start();
                }).show();
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
            getPackageManager().setComponentEnabledSetting(launcher(), hidden
                    ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED : PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
            Toast.makeText(this, hidden ? "桌面图标已隐藏，可从 LSPosed 打开设置" : "桌面图标已恢复", Toast.LENGTH_LONG).show();
        } catch (RuntimeException error) {
            Toast.makeText(this, "无法修改桌面图标：" + error.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
        }
        refreshLauncher();
    }
}

