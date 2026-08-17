package es.vargontoc.storyteller.application.ports.in.generator;

import es.vargontoc.storyteller.domain.model.KindImage;

public interface ImageGeneration {
    
    byte[] generateImage(KindImage kind, long id);
}
