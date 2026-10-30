package dev.kaede.pixelboard;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import org.xmlpull.v1.XmlPullParser;

/** Detect equivalent native controls from installed preference XML, never from a version threshold. */
final class NativeSettingsCatalog {
    private static final String GBOARD="com.google.android.inputmethod.latin", ANDROID="http://schemas.android.com/apk/res/android";
    static final class Result {
        final String fingerprint;
        final Set<String> nativeKeys;
        final Set<UpstreamFeature> owned;
        final boolean retention;
        Result(String fingerprint,Map<String,String> controls,Set<String> hours) {
            this.fingerprint=fingerprint;
            nativeKeys=Collections.unmodifiableSet(new HashSet<>(controls.keySet()));
            Set<UpstreamFeature> features=EnumSet.noneOf(UpstreamFeature.class);
            if(isSwitch(controls.get("enable_fast_access_bar"))) features.add(UpstreamFeature.QUICK_EMOJI);
            if(isSwitch(controls.get("enable_emojify"))) features.add(UpstreamFeature.MAGIC_EMOJI);
            if(controls.getOrDefault("keyboard_emoji_scale_setting", "").endsWith("EmojiScaleSliderPreference")) features.add(UpstreamFeature.EMOJI_SIZE);
            owned=Collections.unmodifiableSet(features);
            retention=controls.getOrDefault(ClipboardRetention.NATIVE_KEY, "").endsWith("LabelSummarySliderPreference")
                    && hours.containsAll(Arrays.asList("1","24","168","720","876000"));
        }
        boolean owns(UpstreamFeature feature) { return owned.contains(feature); }
        private static boolean isSwitch(String type) { return type!=null && type.contains("SwitchPreference"); }
    }
    static final Result EMPTY=new Result("unavailable",Collections.emptyMap(),Collections.emptySet());
    private static volatile Result cached=EMPTY;
    private NativeSettingsCatalog() {}
    static Result detect(Context context) {
        try {
            ApplicationInfo app=context.getPackageManager().getApplicationInfo(GBOARD,0);
            List<String> paths=new ArrayList<>(); paths.add(app.sourceDir);
            if(app.splitSourceDirs!=null) paths.addAll(Arrays.asList(app.splitSourceDirs));
            StringBuilder version=new StringBuilder("native-controls-1");
            for(String path:paths) { File f=new File(path); version.append('|').append(path).append(':').append(f.length()).append(':').append(f.lastModified()); }
            String key=version.toString(); Result previous=cached;
            if(key.equals(previous.fingerprint)) return previous;
            android.content.SharedPreferences cache=context.getSharedPreferences("native_settings_catalog",0);
            if(key.equals(cache.getString("fingerprint",""))) {
                Map<String,String> controls=new HashMap<>();
                for(String item:cache.getStringSet("controls",Collections.emptySet())) {
                    int cut=item.indexOf('='); if(cut>0) controls.put(item.substring(0,cut),item.substring(cut+1));
                }
                Result result=new Result(key,controls,cache.getStringSet("hours",Collections.emptySet())); cached=result; return result;
            }
            Resources resources=context.getPackageManager().getResourcesForApplication(app);
            Map<String,String> controls=new HashMap<>(); Set<String> hours=new HashSet<>();
            for(String path:paths) {
                try(ZipFile zip=new ZipFile(path)) {
                    Enumeration<? extends ZipEntry> entries=zip.entries();
                    while(entries.hasMoreElements()) {
                        ZipEntry entry=entries.nextElement(); String name=entry.getName();
                        if(!name.startsWith("res/") || !name.endsWith(".xml") || entry.getSize()>1024*1024) continue;
                        // Preference XML filenames may also be obfuscated. Inspect only candidates containing a preference type.
                        byte[] bytes=read(zip,entry);
                        if(!candidate(bytes)) continue;
                        try(XmlResourceParser xml=resources.getAssets().openXmlResourceParser(name)) {
                            while(xml.next()!=XmlPullParser.END_DOCUMENT) {
                                if(xml.getEventType()!=XmlPullParser.START_TAG || !xml.getName().contains("Preference")) continue;
                                int id=xml.getAttributeResourceValue(ANDROID,"key",0);
                                String nativeKey=id==0?xml.getAttributeValue(ANDROID,"key"):resources.getString(id);
                                if(nativeKey==null || nativeKey.isEmpty()) continue;
                                String type=xml.getName();
                                if(type.contains("SwitchPreference") || type.endsWith("SliderPreference")) controls.put(nativeKey,type);
                                if(ClipboardRetention.NATIVE_KEY.equals(nativeKey) && type.endsWith("LabelSummarySliderPreference")) {
                                    int values=xml.getAttributeResourceValue(ANDROID,"entryValues",0);
                                    if(values!=0) hours.addAll(Arrays.asList(resources.getStringArray(values)));
                                }
                            }
                        }
                    }
                }
            }
            Result result=new Result(key,controls,hours); Set<String> encoded=new HashSet<>();
            for(Map.Entry<String,String> control:controls.entrySet()) encoded.add(control.getKey()+"="+control.getValue());
            cache.edit().clear().putString("fingerprint",key).putStringSet("controls",encoded).putStringSet("hours",hours).apply();
            cached=result; return result;
        } catch(Exception | LinkageError error) {
            // Unknown / inaccessible settings never cause a module control to disappear.
            android.util.Log.w("PixelBoardLS","Native settings discovery unavailable; module controls retained",error);
            return EMPTY;
        }
    }
    static boolean candidate(byte[] bytes) {
        return new String(bytes,StandardCharsets.UTF_8).contains("Preference")
                || new String(bytes,StandardCharsets.UTF_16LE).contains("Preference");
    }
    private static byte[] read(ZipFile zip,ZipEntry entry) throws IOException {
        try(InputStream in=zip.getInputStream(entry); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[4096]; int n;
            while((n=in.read(buffer))!=-1) { if(out.size()>1024*1024-n) throw new IOException("Preference XML too large"); out.write(buffer,0,n); }
            return out.toByteArray();
        }
    }
}
