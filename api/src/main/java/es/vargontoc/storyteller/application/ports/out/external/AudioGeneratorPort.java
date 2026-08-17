package es.vargontoc.storyteller.application.ports.out.external;

import es.vargontoc.storyteller.domain.request.AudioGenerationRequest;

public interface AudioGeneratorPort {
    
    byte[] generateAudio(AudioGenerationRequest request);
}
