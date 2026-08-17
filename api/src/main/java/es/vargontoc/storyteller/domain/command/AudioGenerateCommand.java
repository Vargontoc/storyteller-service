package es.vargontoc.storyteller.domain.command;

import es.vargontoc.storyteller.domain.enums.VoiceTonePreset;
import es.vargontoc.storyteller.domain.model.AudioToneParams;

public record AudioGenerateCommand(
    long pageId,
    String voiceName,
    VoiceTonePreset preset,
    AudioToneParams customParams
) {
    
}
