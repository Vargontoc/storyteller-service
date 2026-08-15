package es.vargontoc.storyteller.ports.out;

import es.vargontoc.storyteller.domain.request.ImageGenerationRequest;

public interface ImageGeneratorPort {
    
    byte[] generateImage(ImageGenerationRequest request);
}
