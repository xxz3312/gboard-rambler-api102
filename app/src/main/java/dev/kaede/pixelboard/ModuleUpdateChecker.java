package dev.kaede.pixelboard;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Foreground manual check only: no account, background polling, download or installation. */
public final class ModuleUpdateChecker {
    private ModuleUpdateChecker() {}
    public static ModuleRelease fetch(String currentVersion) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(
                "https://api.github.com/repos/" + ModuleRelease.REPOSITORY + "/releases/latest").openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
        connection.setRequestProperty("User-Agent", "Gboard-Rambler-API102/" + currentVersion);
        try {
            int code = connection.getResponseCode();
            if (code == 403 || code == 429) throw new IOException("GitHub 请求受限，请稍后重试");
            if (code == 404) throw new IOException("尚未找到正式版发布");
            if (code != 200) throw new IOException("更新服务响应异常（HTTP " + code + "）");
            try (InputStream input = connection.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    if (Thread.currentThread().isInterrupted()) throw new IOException("检查已取消");
                    if (output.size() + count > 1024 * 1024) throw new IOException("发布信息过大");
                    output.write(buffer, 0, count);
                }
                return ModuleRelease.parse(output.toString(StandardCharsets.UTF_8.name()));
            }
        } finally { connection.disconnect(); }
    }
}
