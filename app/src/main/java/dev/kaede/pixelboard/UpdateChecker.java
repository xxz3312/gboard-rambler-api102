package dev.kaede.pixelboard;

import org.json.JSONObject;
import java.io.*;
import java.net.*;
import javax.net.ssl.HttpsURLConnection;
import java.nio.charset.StandardCharsets;
import java.math.BigInteger;

/** Manual stable-release checks. Never downloads or installs an APK automatically. */
public final class UpdateChecker {
    static final String API = "https://api.github.com/repos/xxz3312/gboard-rambler-api102/releases/latest";
    static final String RELEASES = "https://github.com/xxz3312/gboard-rambler-api102/releases";
    private static final int MAX_BODY = 262144;
    private UpdateChecker() {}
    public enum Status { NEWER, CURRENT, AHEAD }
    public static final class Result {
        public final String version, notes, url;
        public final Status status;
        private Result(String version, String notes, String url, Status status) {
            this.version=version; this.notes=notes; this.url=url; this.status=status;
        }
    }
    static String numeric(String version) {
        if (version == null || version.length()>64 || !version.matches("[vV]?[0-9]+(?:\\.[0-9]+)*"))
            throw new IllegalArgumentException("版本号格式无效");
        return version.replaceFirst("^[vV]", "");
    }
    static int compareVersions(String left, String right) {
        String[] a=numeric(left).split("\\."), b=numeric(right).split("\\.");
        for (int i=0;i<Math.max(a.length,b.length);i++) {
            int compared=new BigInteger(i<a.length?a[i]:"0").compareTo(new BigInteger(i<b.length?b[i]:"0"));
            if (compared!=0) return compared;
        }
        return 0;
    }
    static Result parse(String json, String installed) throws Exception {
        JSONObject release=new JSONObject(json);
        if (release.getBoolean("draft") || release.getBoolean("prerelease"))
            throw new IOException("未找到可用的正式版本");
        String tag=release.getString("tag_name");
        String version=numeric(tag);
        int compared=compareVersions(version,installed);
        String notes=release.optString("body", "").trim();
        if (notes.length()>4000) notes=notes.substring(0,4000)+"…";
        // Build the trusted destination from the validated numeric tag, not an API-supplied URL.
        return new Result(version,notes,RELEASES+"/tag/"+tag,
                compared>0?Status.NEWER:compared<0?Status.AHEAD:Status.CURRENT);
    }
    static String readBody(InputStream stream) throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        byte[] buffer=new byte[8192]; int count;
        while ((count=stream.read(buffer))!=-1) {
            if (out.size()+count>MAX_BODY) throw new IOException("更新响应过大");
            out.write(buffer,0,count);
        }
        return out.toString(StandardCharsets.UTF_8.name());
    }
    public static Result check(String installed) throws Exception {
        HttpsURLConnection connection=(HttpsURLConnection)new URL(API).openConnection();
        connection.setConnectTimeout(8000); connection.setReadTimeout(8000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("User-Agent", "Gboard-Rambler/"+numeric(installed));
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
        try {
            int status=connection.getResponseCode();
            if (status==404) throw new IOException("仓库尚无正式 Release");
            if (status==403 || status==429) throw new IOException("GitHub 请求受限，请稍后重试");
            if (status!=200) throw new IOException("更新检查失败（HTTP "+status+"）");
            try (InputStream stream=connection.getInputStream()) { return parse(readBody(stream),installed); }
        } finally { connection.disconnect(); }
    }
}
