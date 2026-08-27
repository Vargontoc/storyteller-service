package es.vargontoc.storyteller.domain.request;

import es.vargontoc.storyteller.domain.enums.VoiceTonePreset;
import es.vargontoc.storyteller.domain.model.AudioToneParams;

public record AudioGenerationRequest(
    String text,
    String voiceName,
    double exageration,
    double cfgWeight,
    double temperature
) {
    public static AudioGenerationRequest withParams(String text, AudioToneParams params){
        return new AudioGenerationRequest(text, null, params.exageration(), params.cfgWeight(), params.temperature());
    }

    public static AudioGenerationRequest reduce(String text, VoiceTonePreset preset){
        return new AudioGenerationRequest(text, null, preset.toParams().exageration(), preset.toParams().cfgWeight(), preset.toParams().temperature());
    }
}
