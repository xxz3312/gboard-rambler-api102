package dev.kaede.pixelboard;

import java.io.IOException;
import java.net.URI;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Validates this module's public GitHub release metadata, never upstream PixelBoard APKs. */
public final class ModuleRelease {
    static final String REPOSITORY = "xxz3312/gboard-rambler-api102";
    public final String version, notes, pageUrl, apkUrl;
    private ModuleRelease(String version, String notes, String pageUrl, String apkUrl) {
        this.version = version; this.notes = notes; this.pageUrl = pageUrl; this.apkUrl = apkUrl;
    }
    static String version(String value) {
        if (value == null || !value.matches("v?[0-9]{1,9}\\.[0-9]{1,9}\\.[0-9]{1,9}"))
            throw new IllegalArgumentException("Invalid stable version");
        return value.startsWith("v") ? value.substring(1) : value;
    }
    public static int compare(String left, String right) {
        String[] a = version(left).split("\\."), b = version(right).split("\\.");
        for (int i = 0; i < a.length; i++) {
            int result = Integer.compare(Integer.parseInt(a[i]), Integer.parseInt(b[i]));
            if (result != 0) return result;
        }
        return 0;
    }
    static boolean validApkUrl(String url, String tag, String name) {
        try {
            URI uri = URI.create(url);
            return "https".equals(uri.getScheme()) && "github.com".equals(uri.getHost())
                    && uri.getPort() == -1 && uri.getUserInfo() == null && uri.getQuery() == null
                    && uri.getFragment() == null && ("/" + REPOSITORY + "/releases/download/" + tag + "/" + name)
                            .equals(uri.getPath());
        } catch (IllegalArgumentException error) { return false; }
    }
    public static ModuleRelease parse(String json) throws IOException {
        try {
            JSONObject release = new JSONObject(json);
            if (release.getBoolean("draft") || release.getBoolean("prerelease"))
                throw new IOException("不是正式版发布");
            String tag = release.getString("tag_name");
            String version = version(tag);
            String name = "Gboard-Rambler-API102-" + version + ".apk";
            String apk = null;
            JSONArray assets = release.getJSONArray("assets");
            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.getJSONObject(i);
                if (name.equals(asset.optString("name")) && asset.optLong("size", 0) > 0) {
                    String url = asset.getString("browser_download_url");
                    if (!validApkUrl(url, tag, name) || apk != null) throw new IOException("APK 下载地址异常");
                    apk = url;
                }
            }
            if (apk == null) throw new IOException("正式版尚未提供匹配的模块 APK");
            String notes = release.optString("body", "").trim();
            if (notes.length() > 2000) notes = notes.substring(0, 2000) + "…";
            return new ModuleRelease(version, notes,
                    "https://github.com/" + REPOSITORY + "/releases/tag/" + tag, apk);
        } catch (JSONException | IllegalArgumentException error) {
            throw new IOException("无法识别发布信息", error);
        }
    }
}
