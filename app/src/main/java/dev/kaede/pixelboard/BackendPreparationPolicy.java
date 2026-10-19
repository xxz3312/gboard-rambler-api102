package dev.kaede.pixelboard;

/** Independent opt-in for local model delivery; never enables Gboard V2 or inference. */
final class BackendPreparationPolicy {
    private BackendPreparationPolicy() {}
    static boolean independentPcsPreparation(String packageName, boolean requested) {
        return requested && "com.google.android.as.oss".equals(packageName);
    }
}
