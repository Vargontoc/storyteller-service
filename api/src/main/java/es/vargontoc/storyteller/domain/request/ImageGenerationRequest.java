package es.vargontoc.storyteller.domain.request;

import java.nio.file.Path;
import java.util.List;

import es.vargontoc.storyteller.domain.enums.KindImage;

public record ImageGenerationRequest(
    KindImage kind,
    String visualDescription,
    List<String> attributes,
    Long seed, 
    List<Path> references
) { 

    public static ImageGenerationRequest actor(String visual, List<String> atributes){
        return new ImageGenerationRequest(KindImage.ACTOR, visual, atributes,null, List.of());
    }

    public static ImageGenerationRequest cover(String visual, List<Path> references){
        return new ImageGenerationRequest(KindImage.COVER, visual, List.of(),  null, List.of());
    }

    public static ImageGenerationRequest page(String visual, List<Path> references){
        return new ImageGenerationRequest(KindImage.PAGE, visual, List.of(), null, List.of());
    }
}
