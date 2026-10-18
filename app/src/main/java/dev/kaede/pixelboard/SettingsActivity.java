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
        addToggle(list,"rambler","Rambler (experimental)","May alter eligible Gboard feature flags; not verified.");
        addToggle(list,"spellchecker","Inline spellchecker (experimental)","Obfuscated method mapping needs on-device validation.");
        addToggle(list,"diagnostics","Diagnostic hooks","Observe selection and emit Android logcat messages.");
        addToggle(list,"writing_tools","AI 写作工具 · 基础云端测试版","使用 Google 云端校对/改写，关闭 V2 和本地推理。基础云端写作已通过用户实测；修改后需强制停止并重新打开 Gboard。");
        addText(list, "LSPosed framework service: " + (((ModuleApp)getApplication()).connected() ? "connected" : "not connected") + ". Configuration sync is retried upon connection.", false);
        addText(list, "Logcat filter: PixelBoardLS. Toggle state is not proof of successful Hook installation.", false);
        android.widget.Button aiCore = new android.widget.Button(this);
        aiCore.setText("检测 AICore 本地写作能力");
        aiCore.setOnClickListener(v -> startActivity(
                new android.content.Intent(this, AiCoreProbeActivity.class)));
        list.addView(aiCore);
        setContentView(root);
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
