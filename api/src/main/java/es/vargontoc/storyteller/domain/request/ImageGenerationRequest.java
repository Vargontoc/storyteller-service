package es.vargontoc.storyteller.domain.request;

import es.vargontoc.storyteller.domain.model.KindImage;

public record ImageGenerationRequest(
    KindImage kind,
    String visualDescription,
    Long seed
) { 

    public static ImageGenerationRequest actor(String visual){
        return new ImageGenerationRequest(KindImage.ACTOR, visual, null);
    }

    public static ImageGenerationRequest cover(String visual){
        return new ImageGenerationRequest(KindImage.COVER, visual, null);
    }

    public static ImageGenerationRequest page(String visual){
        return new ImageGenerationRequest(KindImage.PAGE, visual, null);
    }
}
