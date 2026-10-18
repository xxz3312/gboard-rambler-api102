package dev.kaede.pixelboard;

import java.util.LinkedHashMap;

/** Process-local diagnostic metadata. Latest event of each type, bounded by type count. */
public final class RuntimeEventCache {
    private final LinkedHashMap<String, String> events = new LinkedHashMap<>();
    public synchronized void put(String event, String line) {
        if (event == null || line == null) return;
        events.remove(event);
        events.put(event, line.substring(0, Math.min(512, line.length())));
        while (events.size() > 16) events.remove(events.keySet().iterator().next());
    }
    public synchronized String snapshot() { return String.join("\n", events.values()); }
}
