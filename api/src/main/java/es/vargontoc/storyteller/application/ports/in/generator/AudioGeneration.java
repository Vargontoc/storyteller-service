package es.vargontoc.storyteller.application.ports.in.generator;

import es.vargontoc.storyteller.domain.command.AudioGenerateCommand;

public interface AudioGeneration {
    
    byte[] generateAudio(AudioGenerateCommand cmd);
}
