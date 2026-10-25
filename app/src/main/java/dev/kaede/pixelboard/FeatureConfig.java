package dev.kaede.pixelboard;

import android.content.SharedPreferences;
import android.util.Log;
import io.github.libxposed.api.XposedModule;
import java.util.*;

/** A single immutable snapshot; listeners never publish a partially read configuration. */
public final class FeatureConfig {
    private static final String TAG = "PixelBoardLS";
    public static final String GROUP = "features";
    private static volatile Snapshot state = new Snapshot(Collections.emptyMap());
    private static SharedPreferences remote;
    // SharedPreferences retains listeners weakly; keep this listener alive for the host process.
    private static SharedPreferences.OnSharedPreferenceChangeListener listener;
    private static volatile boolean ramblerCompatible, writingCompatible;
    private FeatureConfig() {}
    static final class Snapshot {
        final Set<String> enabled;
        final String retention;
        Snapshot(Map<String, ?> values) {
            Set<String> selected = new HashSet<>();
            for (Map.Entry<String, ?> item : values.entrySet())
                if (Boolean.TRUE.equals(item.getValue())) selected.add(item.getKey());
            enabled = Collections.unmodifiableSet(selected);
            retention = ClipboardRetention.sanitize(values.get(ClipboardRetention.KEY));
        }
        boolean has(String name) { return enabled.contains(name); }
    }
    public static void initialize(XposedModule module) {
        state = new Snapshot(Collections.emptyMap());
        try {
            remote = module.getRemotePreferences(GROUP);
            reload(remote);
            listener = (prefs, key) -> reload(prefs);
            remote.registerOnSharedPreferenceChangeListener(listener);
            reload(remote);
            module.log(Log.INFO, TAG, "Remote settings loaded; live policy listener registered");
        } catch (RuntimeException e) {
            module.log(Log.ERROR, TAG, "Remote settings unavailable; defaults retained", e);
        }
    }
    static void reload(SharedPreferences prefs) {
        try { state = new Snapshot(prefs.getAll()); }
        catch (RuntimeException error) { Log.w(TAG, "Cannot reload settings; keeping last snapshot", error); }
    }
    public static boolean enabled(UpstreamFeature feature) { return state.has(feature.key); }
    public static String upstreamSummary() {
        Set<UpstreamFeature> selected = EnumSet.noneOf(UpstreamFeature.class);
        for (UpstreamFeature feature : UpstreamFeature.values()) if (enabled(feature)) selected.add(feature);
        return selected.toString();
    }
    public static String retention() { return state.retention; }
    public static boolean scanText() { return state.has("scan_text"); }
    public static boolean screenshot() { return state.has("screenshot_suggestions"); }
    public static boolean roundedKeys() { return state.has("rounded_keys"); }
    public static boolean extrasRequested() {
        Snapshot current = state;
        if (current.has("scan_text") || current.has("screenshot_suggestions") || current.has("rounded_keys")) return true;
        for (UpstreamFeature feature : UpstreamFeature.values())
            if (feature.usesFlags() && current.has(feature.key)) return true;
        return false;
    }
    public static boolean ramblerRequested() { return state.has("rambler"); }
    public static boolean writingRequested() { return state.has("writing_tools"); }
    public static void compatibility(boolean voice, boolean writing) {
        ramblerCompatible=voice; writingCompatible=writing;
    }
    public static boolean rambler() { return ramblerRequested() && ramblerCompatible; }
    public static boolean diagnostics() { return state.has("diagnostics"); }
    public static boolean writingTools() { return writingRequested() && writingCompatible; }
}
