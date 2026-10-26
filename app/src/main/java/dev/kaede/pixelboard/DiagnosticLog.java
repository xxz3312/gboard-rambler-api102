package dev.kaede.pixelboard;

import android.content.Context;
import android.content.SharedPreferences;

/** Bounded metadata history in the module's private storage; never reads input or clipboard data. */
final class DiagnosticLog {
    static final int MAX_CHARS=65536;
    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences("diagnostic_history",0);
    }
    static String append(String history, String entry) {
        if (entry.length()>2048) entry=entry.substring(0,2048)+"…";
        String result=history+entry+"\n";
        if (result.length()<=MAX_CHARS) return result;
        int start=result.indexOf('\n',result.length()-MAX_CHARS);
        return start<0 ? "" : result.substring(start+1);
    }
    static synchronized void record(Context context, String event, String detail) {
        try {
            SharedPreferences prefs=preferences(context);
            String when=new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS",java.util.Locale.US)
                    .format(new java.util.Date());
            prefs.edit().putString("history",append(prefs.getString("history",""),
                    when+" "+event+" "+detail)).apply();
        } catch (RuntimeException ignored) {}
    }
    static String history(Context context) { return preferences(context).getString("history",""); }
}
