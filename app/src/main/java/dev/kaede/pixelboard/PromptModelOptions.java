package dev.kaede.pixelboard;
import com.google.mlkit.genai.prompt.Generation;
import com.google.mlkit.genai.prompt.GenerationConfig;
import com.google.mlkit.genai.prompt.ModelConfig;
import com.google.mlkit.genai.prompt.ModelPreference;
import com.google.mlkit.genai.prompt.ModelReleaseStage;
import com.google.mlkit.genai.prompt.java.GenerativeModelFutures;

/** Public SDK configurations only. Each check has its own client and original result. */
final class PromptModelOptions {
    static final String[] LABELS={"STABLE / FULL (default)","STABLE / FAST","PREVIEW / FULL","PREVIEW / FAST"};
    private PromptModelOptions() {}
    static String label(int index) { validate(index); return LABELS[index]; }
    static ModelConfig config(int index) {
        validate(index);
        ModelConfig.Builder builder=new ModelConfig.Builder();
        builder.setReleaseStage(index<2 ? ModelReleaseStage.STABLE : ModelReleaseStage.PREVIEW);
        builder.setPreference(index%2==0 ? ModelPreference.FULL : ModelPreference.FAST);
        return builder.build();
    }
    static GenerativeModelFutures client(int index) {
        GenerationConfig.Builder builder=new GenerationConfig.Builder();
        builder.setModelConfig(config(index));
        return GenerativeModelFutures.from(Generation.INSTANCE.getClient(builder.build()));
    }
    private static void validate(int index) {
        if(index<0||index>=LABELS.length)throw new IllegalArgumentException("Unknown model configuration");
    }
}
