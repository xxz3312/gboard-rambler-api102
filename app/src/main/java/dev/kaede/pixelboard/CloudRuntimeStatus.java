package dev.kaede.pixelboard;

import android.app.BroadcastOptions;
import android.content.*;
import android.os.Build;
import android.os.Process;

/** Metadata only. Receiving app authenticates the sender through Android 14+ identity. */
public final class CloudRuntimeStatus extends BroadcastReceiver {
    public static final String VERSION="0.2.2";
    private static final String PREF="cloud_runtime_status";
    public static void send(Context context,String event,String detail) {
        if(context==null||Build.VERSION.SDK_INT<34)return;
        try {
            Intent intent=new Intent().setComponent(new ComponentName(
                    "dev.kaede.pixelboard","dev.kaede.pixelboard.CloudRuntimeStatus"));
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            intent.putExtra("event",event).putExtra("detail",detail).putExtra("version",VERSION)
                    .putExtra("pid",Process.myPid()).putExtra("when",System.currentTimeMillis());
            context.sendBroadcast(intent,null,BroadcastOptions.makeBasic().setShareIdentityEnabled(true).toBundle());
        } catch(RuntimeException|LinkageError ignored) {}
    }
    @Override public void onReceive(Context context,Intent intent) {
        if(Build.VERSION.SDK_INT<34)return;
        if(!"com.google.android.inputmethod.latin".equals(getSentFromPackage())||getSentFromUid()<0)return;
        try {
            int uid=context.getPackageManager().getPackageUid("com.google.android.inputmethod.latin",0);
            if(uid!=getSentFromUid()||!VERSION.equals(intent.getStringExtra("version")))return;
            String event=intent.getStringExtra("event");
            if(!"COMPAT".equals(event)&&!"INIT".equals(event)&&!"GETTER".equals(event)&&!"FLAG".equals(event)&&!"HOOKS".equals(event)&&!"ERROR".equals(event)&&!"EXTRAS".equals(event)&&!"EXTRA_FLAG".equals(event)&&!"UPSTREAM".equals(event)&&!"RUNTIME_HOOKS".equals(event)&&!"CLIPBOARD_SYNC".equals(event)&&!"CONFIG_APPLIED".equals(event)&&!"RAMBLER_SELECTION".equals(event)&&!"RAMBLER_FLAG".equals(event)&&!"RETENTION_READ".equals(event))return;
            String detail=intent.getStringExtra("detail");
            if(detail==null||detail.length()>480)return;
            android.content.SharedPreferences prefs=context.getSharedPreferences(PREF,0);
            android.content.SharedPreferences.Editor edit=prefs.edit();
            int pid=intent.getIntExtra("pid",-1);
            if(pid<1)return;
            // New process cannot inherit a previous process's getter or flag status.
            if("COMPAT".equals(event))edit.clear();
            else if(pid!=prefs.getInt("pid",-1)) {
                if(!"ERROR".equals(event))return;
                edit.clear();
            }
            DiagnosticLog.record(context,event,"version="+VERSION+" pid="+pid+" "+detail);
            edit.putInt("pid",pid).putString("version",VERSION)
                    .putString(event,detail).putLong(event+"_when",intent.getLongExtra("when",0)).apply();
        } catch(Exception ignored) {}
    }
    public static boolean retentionUnavailable(Context context) {
        android.content.SharedPreferences p=context.getSharedPreferences(PREF,0);
        return VERSION.equals(p.getString("version",""))
                && p.getString("RUNTIME_HOOKS","").contains("retentionCalculation=skipped");
    }
    public static boolean unsupported(Context context) {
        android.content.SharedPreferences p=context.getSharedPreferences(PREF,0);
        return VERSION.equals(p.getString("version","")) && (
                p.getString("COMPAT","").contains("false") || !p.getString("ERROR","").isEmpty()
                || installationFailed(p.getString("HOOKS","")));
    }
    static boolean installationFailed(String hooks) {
        return hooks.contains("ramblerRequested=true") && hooks.contains("ramblerInstalled=false")
                || hooks.contains("writingRequested=true") && hooks.contains("writingInstalled=false")
                || hooks.contains("extrasRequested=true") && hooks.contains("extrasInstalled=false");
    }
    public static String report(Context context) {
        android.content.SharedPreferences p=context.getSharedPreferences(PREF,0);
        if(!VERSION.equals(p.getString("version","")))return "尚未收到本版 Gboard 运行事件；不能据此判定版本不支持。请重启并唤起 Gboard，再刷新。";
        String capability=p.getString("COMPAT","");
        StringBuilder out=new StringBuilder();
        if(capability.contains("rambler=false"))out.append("不支持该版本 Gboard：Rambler 未找到唯一匹配的 Hook 点。\n");
        if(capability.contains("writing=false"))out.append("不支持该版本 Gboard：写作工具未找到唯一匹配的 Hook 点。\n");
        String hooks=p.getString("HOOKS","");
        String error=p.getString("ERROR","");
        if(!error.isEmpty())out.append("模块初始化失败；请查看 ERROR，不能据此判断后端可用性。\n");
        else if(installationFailed(hooks))out.append("Hook 安装未完成；已开启功能尚未成功安装全部 Hook。\n");
        else if(hooks.isEmpty())out.append("等待 Hook 安装结果；定位成功不等于已安装。\n");
        else if(!capability.contains("false"))out.append("Hook 安装检查完成；云端功能仍需实际操作验证。\n");
        out.append("Gboard PID=").append(p.getInt("pid",-1)).append("\n");
        for(String event:new String[]{"COMPAT","INIT","GETTER","HOOKS","ERROR","FLAG","EXTRAS","EXTRA_FLAG","UPSTREAM","RUNTIME_HOOKS","CLIPBOARD_SYNC","CONFIG_APPLIED","RAMBLER_SELECTION","RAMBLER_FLAG","RETENTION_READ"}) {
            String detail=p.getString(event,"");
            if(!detail.isEmpty()) {
                long time=p.getLong(event+"_when",0);
                out.append(new java.text.SimpleDateFormat("MM-dd HH:mm:ss",java.util.Locale.US)
                    .format(new java.util.Date(time))).append(" ").append(event).append(" ").append(detail).append("\n");
            }
        }
        return out.toString();
    }
}


