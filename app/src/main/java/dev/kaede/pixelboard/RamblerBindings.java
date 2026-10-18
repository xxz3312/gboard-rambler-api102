package dev.kaede.pixelboard;

/**
 * Names transcribed from Akshayykadam/PixelBoard's OfficialSelectorPatch (GPL-3.0).
 * These are candidate symbols, NOT universal stable identifiers.
 */
public final class RamblerBindings {
    private RamblerBindings() {}
    public static final String VOICE = "com.google.android.apps.inputmethod.latin.preference.VoiceSettingsFragment";
    public static final String SELECTOR = "com.google.android.libraries.inputmethod.preferencewidgets.CustomSelectorWithWidgetPreference";
    public static final class Profile {
        public final String id;
        public final TargetMethod layout, setup, create, write, read, defaults;
        private Profile(String id, String layoutName, String writeName, String writeArg,
                        String readOwner, String defaultOwner, String defaultName) {
            this.id = id;
            layout = new TargetMethod(VOICE, layoutName, "int");
            setup = new TargetMethod(VOICE, "ac", "void");
            create = new TargetMethod(VOICE, "f", "void", "android.os.Bundle");
            write = new TargetMethod(VOICE, writeName, "void", "boolean", writeArg, SELECTOR, SELECTOR);
            read = new TargetMethod(readOwner, "a", "boolean", "android.content.Context");
            defaults = new TargetMethod(defaultOwner, defaultName, "void");
        }
        public TargetMethod[] all() { return new TargetMethod[]{layout,setup,create,write,read,defaults}; }
    }
    public static final Profile[] PROFILES = {
        new Profile("18.4.1", "aC", "aE", "qou", "mvq", "fde", "hT"),
        new Profile("18.3.1", "aD", "aF", "ahbz", "aaeo", "kcm", "ge"),
        new Profile("18.0.3", "aB", "aD", "qhy", "mqk", "fbl", "hN")
    };

    /** Fail closed: require the whole signature set, never match by name alone. */
    public static Profile select(ClassLoader loader) {
        Profile matched = null;
        for (Profile profile : PROFILES) {
            try {
                for (TargetMethod m : profile.all()) m.resolve(loader);
                if (matched != null) return null; // ambiguous: no patching
                matched = profile;
            } catch (ReflectiveOperationException | LinkageError e) {
                // Different Gboard build: skip this profile.
            }
        }
        return matched;
    }
}
