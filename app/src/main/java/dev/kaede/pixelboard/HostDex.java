package dev.kaede.pixelboard;

import android.content.Context;
import java.util.*;
import java.io.IOException;
import java.util.zip.ZipFile;
import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.result.MethodData;

/** Search the installed base and every split, with descriptor deduplication. */
final class HostDex implements AutoCloseable {
    private final List<DexKitBridge> bridges = new ArrayList<>();
    HostDex(Context context) {
        System.loadLibrary("dexkit");
        List<String> paths = new ArrayList<>();
        paths.add(context.getApplicationInfo().sourceDir);
        String[] splits = context.getApplicationInfo().splitSourceDirs;
        if (splits != null) paths.addAll(Arrays.asList(splits));
        try {
            for (String path : new LinkedHashSet<>(paths))
                if (containsDex(path)) bridges.add(DexKitBridge.create(path));
        } catch (RuntimeException | LinkageError error) { close(); throw error; }
    }
    static boolean containsDex(String path) {
        try (ZipFile zip = new ZipFile(path)) {
            return zip.stream().anyMatch(entry -> !entry.isDirectory()
                    && entry.getName().matches("classes(?:[2-9]|[1-9][0-9]+)?\\.dex"));
        } catch (IOException error) { throw new IllegalArgumentException("Unreadable APK", error); }
    }
    List<MethodData> findMethod(FindMethod query) {
        Map<String, MethodData> methods = new LinkedHashMap<>();
        for (DexKitBridge bridge : bridges) for (MethodData method : bridge.findMethod(query)) {
            String descriptor = method.getClassName()+"#"+method.getMethodName()
                    +method.getParamTypeNames()+":"+method.getReturnTypeName();
            methods.putIfAbsent(descriptor, method);
        }
        return new ArrayList<>(methods.values());
    }
    @Override public void close() {
        for (DexKitBridge bridge : bridges) bridge.close();
        bridges.clear();
    }
}
