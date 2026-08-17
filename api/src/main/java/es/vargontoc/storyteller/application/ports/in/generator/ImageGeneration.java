package es.vargontoc.storyteller.application.ports.in.generator;

import es.vargontoc.storyteller.domain.enums.KindImage;

public interface ImageGeneration {
    
    byte[] generateImage(KindImage kind, long id);
}
