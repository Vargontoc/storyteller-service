package es.vargontoc.storyteller.domain.request;

import java.nio.file.Path;
import java.util.List;

import es.vargontoc.storyteller.domain.enums.KindImage;

public record ImageGenerationRequest(
    KindImage kind,
    String visualDescription,
    Long seed, 
    List<Path> references
) { 

    public static ImageGenerationRequest actor(String visual){
        return new ImageGenerationRequest(KindImage.ACTOR, visual, null, List.of());
    }

    public static ImageGenerationRequest cover(String visual, List<Path> references){
        return new ImageGenerationRequest(KindImage.COVER, visual, null, List.of());
    }

    public static ImageGenerationRequest page(String visual, List<Path> references){
        return new ImageGenerationRequest(KindImage.PAGE, visual, null, List.of());
    }
}
