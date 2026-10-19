package dev.kaede.pixelboard;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.InputFilter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.genai.common.DownloadCallback;
import com.google.mlkit.genai.common.FeatureStatus;
import com.google.mlkit.genai.common.GenAiException;
import com.google.mlkit.genai.prompt.GenerateContentRequest;
import com.google.mlkit.genai.prompt.Generation;
import com.google.mlkit.genai.prompt.TextPart;
import com.google.mlkit.genai.prompt.java.GenerativeModelFutures;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.function.Consumer;

/** Foreground, real on-device generation. Independent of Gboard's private feature 703. */
public final class AiCoreLocalActivity extends Activity {
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ArrayList<Button> actions = new ArrayList<>();
    private GenerativeModelFutures model;
    private ListenableFuture<?> pending;
    private Runnable deadline;
    private long serial;
    private long started;
    private boolean visible;
    private boolean busy;
    private TextView report;
    private TextView output;
    private EditText input;
    private String metadata = "";

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle("AICore 端侧执行");
        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        list.setPadding(pad, pad, pad, pad);
        scroll.addView(list);
        text(list, "基础模型 · Prompt API", 22);
        text(list, "直接使用 AICore 在本机生成文本。下载模型需要联网，并可能占用较多流量和存储。"
                + "首次使用：查询 → 必要时下载 → 执行。执行时请保持本页在前台。", 15);
        text(list, "这条路径独立于 Gboard V2 私有接口；本页成功仍需后续接入 Gboard。"
                + "不自动回退云端，不记录或保存输入、输出。", 14);
        input = new EditText(this);
        input.setFilters(new InputFilter[] {new InputFilter.LengthFilter(1000)});
        input.setMinLines(2);
        input.setHint("输入英语短文，最多 1000 字符");
        input.setText("I has a meeting tomorrow.");
        input.setSaveEnabled(false);
        list.addView(input);
        button(list, "查询基础模型与状态", () -> begin(0));
        button(list, "下载 / 等待模型准备", () -> begin(1));
        button(list, "执行英语校对（端侧）", () -> begin(2));
        button(list, "执行正式语气改写（端侧）", () -> begin(3));
        Button cancel = new Button(this);
        cancel.setText("取消等待");
        cancel.setOnClickListener(v -> {
            if (busy) {
                invalidate();
                closeModel();
                append("已取消等待；AICore 系统下载可能继续。尚未确认推理成功。");
            }
        });
        list.addView(cancel);
        report = text(list, "尚未查询。", 14);
        report.setTextIsSelectable(true);
        output = text(list, "模型输出将在此显示。", 18);
        output.setTextIsSelectable(true);
        output.setSaveEnabled(false);
        Button copy = new Button(this);
        copy.setText("复制诊断报告（不含输入和输出）");
        copy.setOnClickListener(v -> {
            ClipboardManager cb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (cb != null) cb.setPrimaryClip(ClipData.newPlainText("AICore 端侧诊断", report.getText()));
        });
        list.addView(copy);
        setContentView(scroll);
    }

    @Override protected void onStart() {
        super.onStart();
        visible = true;
        setBusy(false);
    }

    @Override protected void onStop() {
        visible = false;
        boolean wasBusy = busy;
        invalidate();
        closeModel();
        if (wasBusy) append("页面已离开前台，已取消本次等待。请返回后重新执行。");
        super.onStop();
    }

    private void begin(int action) {
        if (busy || !visible) return;
        final String draft = input.getText().toString().trim();
        if (action >= 2 && draft.isEmpty()) {
            input.setError("请输入英语短文");
            return;
        }
        invalidate();
        long token = serial;
        started = SystemClock.elapsedRealtime();
        metadata = environment() + "\n操作=" + action
                + "（0查询 / 1下载 / 2校对 / 3正式改写）\n";
        report.setText(metadata);
        output.setText("本次尚无模型输出。");
        setBusy(true);
        try {
            if (model == null) model = GenerativeModelFutures.from(Generation.INSTANCE.getClient());
            append("正在查询基础模型状态…");
            await(model.checkStatus(), token, 30000, status -> {
                append("Prompt 状态=" + statusName(status));
                // A model-name query is diagnostic only; failure must not turn AVAILABLE into success/failure.
                try {
                    await(model.getBaseModelName(), token, 15000,
                            name -> { append("基础模型=" + name); afterStatus(action, draft, status, token); },
                            error -> { append("基础模型名称查询失败：" + describe(error));
                                afterStatus(action, draft, status, token); });
                } catch (RuntimeException error) {
                    append("基础模型名称查询失败：" + describe(error));
                    afterStatus(action, draft, status, token);
                }
            }, this::fail);
        } catch (RuntimeException error) { fail(error); }
    }

    private void afterStatus(int action, String draft, int status, long token) {
        if (!current(token)) return;
        if (action == 0) {
            finish("查询完成；尚未执行推理。");
        } else if (action == 1 && (status == FeatureStatus.DOWNLOADABLE
                || status == FeatureStatus.DOWNLOADING)) {
            try {
                append("等待 AICore 下载模型，最多 15 分钟。可取消等待。");
                await(model.download(new DownloadCallback() {
                    @Override public void onDownloadStarted(long bytes) {
                        progress(token, "下载开始，字节数=" + bytes);
                    }
                    @Override public void onDownloadProgress(long bytes) {
                        // Replace progress instead of retaining an unbounded callback history.
                        main.post(() -> { if (current(token)) report.setText(metadata
                                + "\n已下载字节数=" + bytes); });
                    }
                    @Override public void onDownloadCompleted() {
                        progress(token, "下载回调完成，正在重新检查状态…");
                    }
                    @Override public void onDownloadFailed(GenAiException error) {
                        progress(token, "下载失败回调：" + describe(error));
                    }
                }), token, 900000, unused -> {
                    try {
                        await(model.checkStatus(), token, 30000,
                                next -> finish("下载等待结束；当前状态=" + statusName(next)
                                        + "。请点击执行验证推理。"), this::fail);
                    } catch (RuntimeException error) { fail(error); }
                }, this::fail);
            } catch (RuntimeException error) { fail(error); }
        } else if (action == 1) {
            finish(status == FeatureStatus.AVAILABLE ? "模型已可用，无需下载；请点击执行。"
                    : "当前状态不允许请求下载；保留原始返回状态。");
        } else if (status != FeatureStatus.AVAILABLE) {
            finish("未执行推理：需要 AVAILABLE；DOWNLOADABLE 时请先点击下载。"
                    + "UNAVAILABLE 或异常需检查 AICore 配置和设备支持。");
        } else {
            try {
                String instruction = action == 2
                        ? "Correct the grammar and spelling of the following English text. Preserve its meaning."
                        : "Rewrite the following English text in a formal professional tone. Preserve its meaning.";
                GenerateContentRequest.Builder builder = new GenerateContentRequest.Builder(new TextPart(
                        instruction + " Return only the revised text, without explanations.\n\nText:\n" + draft));
                builder.setTemperature(0.2f);
                builder.setMaxOutputTokens(256);
                GenerateContentRequest request = builder.build();
                append("正在端侧生成，最多等待 120 秒…");
                await(model.generateContent(request), token, 120000, response -> {
                    if (response.getCandidates().isEmpty()) {
                        finish("INFERENCE_EMPTY：调用结束，但没有候选文本。");
                        return;
                    }
                    String result = response.getCandidates().get(0).getText();
                    if (result == null || result.trim().isEmpty()) {
                        finish("INFERENCE_EMPTY：候选文本为空；不能判定成功。");
                        return;
                    }
                    output.setText(result);
                    finish("INFERENCE_RETURNED_TEXT finishReason="
                            + response.getCandidates().get(0).getFinishReason()
                            + "\n本次返回了模型文本，请检查结果质量；离线能力需断网后再次执行确认。");
                }, this::fail);
            } catch (RuntimeException error) { fail(error); }
        }
    }

    /** Listener only calls get() after completion; no main-thread blocking waits. */
    private <T> void await(ListenableFuture<T> future, long token, long timeoutMs,
                           Consumer<T> success, Consumer<Throwable> failure) {
        pending = future;
        Runnable timeout = () -> {
            if (!current(token) || pending != future) return;
            pending = null;
            future.cancel(true);
            failure.accept(new java.util.concurrent.TimeoutException("等待 " + timeoutMs / 1000 + " 秒超时"));
        };
        deadline = timeout;
        main.postDelayed(timeout, timeoutMs);
        future.addListener(() -> main.post(() -> {
            if (!current(token) || pending != future) return;
            main.removeCallbacks(timeout);
            deadline = null;
            pending = null;
            T value;
            try { value = future.get(); }
            catch (Exception error) { failure.accept(error); return; }
            success.accept(value);
        }), Runnable::run);
    }

    private boolean current(long token) { return visible && busy && serial == token; }
    private void progress(long token, String message) {
        main.post(() -> { if (current(token)) append(message); });
    }
    private void invalidate() {
        serial++;
        if (deadline != null) main.removeCallbacks(deadline);
        deadline = null;
        ListenableFuture<?> old = pending;
        pending = null;
        if (old != null) old.cancel(true);
        setBusy(false);
    }
    private void fail(Throwable error) {
        finish("操作失败：" + describe(error));
        closeModel();
    }
    private void finish(String message) {
        append(message + "\n耗时=" + (SystemClock.elapsedRealtime() - started) + " ms");
        invalidate();
    }
    private void closeModel() {
        if (model != null) {
            try { model.getGenerativeModel().close(); } catch (RuntimeException ignored) { }
            model = null;
        }
    }
    private void setBusy(boolean value) {
        busy = value;
        for (Button button : actions) button.setEnabled(!value);
        if (input != null) input.setEnabled(!value);
    }
    private void append(String message) {
        metadata += message + "\n";
        report.setText(metadata);
    }
    private String environment() {
        String version;
        try {
            PackageInfo info = getPackageManager().getPackageInfo("com.google.android.aicore", 0);
            version = info.versionName + " (" + info.getLongVersionCode() + ")";
        } catch (android.content.pm.PackageManager.NameNotFoundException error) { version = "未安装"; }
        return "模块=" + BuildConfig.VERSION_NAME + "\n设备=" + Build.MANUFACTURER + "/" + Build.MODEL
                + " API=" + Build.VERSION.SDK_INT + " SoC=" + (Build.VERSION.SDK_INT >= 31 ? Build.SOC_MODEL : "未知")
                + "\nAICore=" + version + "\nAICORE_QC="
                + getPackageManager().hasSystemFeature("com.google.android.feature.AICORE_QC")
                + " SM8850=" + getPackageManager().hasSystemFeature("com.google.android.feature.AICORE_QC_SM8850")
                + "\n模型下载准备请求=" + getSharedPreferences("features", MODE_PRIVATE)
                        .getBoolean("local_model_preparation", false)
                + "（保存值，PCS 生效以 ASTREA_FLAG 日志为准）"
                + "\n执行路径=ML Kit Prompt API（独立前台调用，无云端回退）";
    }
    private static String statusName(int status) {
        if (status == FeatureStatus.AVAILABLE) return "AVAILABLE";
        if (status == FeatureStatus.DOWNLOADABLE) return "DOWNLOADABLE";
        if (status == FeatureStatus.DOWNLOADING) return "DOWNLOADING";
        if (status == FeatureStatus.UNAVAILABLE) return "UNAVAILABLE";
        return "UNKNOWN(" + status + ")";
    }
    private static String describe(Throwable error) {
        StringBuilder result = new StringBuilder();
        IdentityHashMap<Throwable, Boolean> seen = new IdentityHashMap<>();
        while (error != null && seen.size() < 5 && seen.put(error, true) == null) {
            if (result.length() > 0) result.append("\n原因：");
            result.append(error.getClass().getSimpleName()).append(": ").append(error.getMessage());
            error = error.getCause();
        }
        return result.toString();
    }
    private void button(LinearLayout list, String label, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setOnClickListener(v -> action.run());
        actions.add(button);
        list.addView(button);
    }
    private TextView text(LinearLayout list, String value, int size) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setPadding(0, 10, 0, 10);
        list.addView(text);
        return text;
    }
}
