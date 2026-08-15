package es.vargontoc.storyteller.application.ports.out.external;

import es.vargontoc.storyteller.domain.request.ImageGenerationRequest;

public interface ImageGeneratorPort {
    
    byte[] generateImage(ImageGenerationRequest request);
}
