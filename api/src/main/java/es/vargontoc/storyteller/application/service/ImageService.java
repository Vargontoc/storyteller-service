package es.vargontoc.storyteller.application.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import es.vargontoc.storyteller.application.ports.in.generator.ImageGeneration;
import es.vargontoc.storyteller.application.ports.out.external.ImageGeneratorPort;
import es.vargontoc.storyteller.application.ports.out.persistence.CharacterRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryPageRepository;
import es.vargontoc.storyteller.domain.model.KindImage;
import es.vargontoc.storyteller.domain.request.ImageGenerationRequest;
import es.vargontoc.storyteller.infrastructure.storage.CharacterImageStorage;
import es.vargontoc.storyteller.shared.exceptions.AppException;

@Service
public class ImageService implements ImageGeneration {

    private final CharacterRepository actorUseCase;
    private final StoryPageRepository pageUseCase;

    private final ImageGeneratorPort generator;
    private final CharacterImageStorage storage;



    public ImageService(CharacterRepository actorUseCase, StoryPageRepository pageUseCase, ImageGeneratorPort generator,
            CharacterImageStorage storage) {
        this.actorUseCase = actorUseCase;
        this.pageUseCase = pageUseCase;
        this.generator = generator;
        this.storage = storage;
    }


    @Override
    public byte[] generateImage(KindImage kind, long id) {

        ImageResult image = new ImageResult();
        if(kind == KindImage.ACTOR) {
            var actor = actorUseCase.getActor(id);
            image.storyId = actor.getStoryId();
            String prompt = actor.getVisualDescription();
            image.request = ImageGenerationRequest.actor(prompt);
        }
        else{
            var page = pageUseCase.getPage(id);
            if(kind == KindImage.PAGE && page.getPage() == 0)
                throw new AppException("El id proporcionado no pertenece a una página", HttpStatus.CONFLICT);
            if(kind == KindImage.COVER && page.getPage() != 0)
                throw new AppException("El id proporcionado no pertenece a una portada", HttpStatus.CONFLICT);
            image.storyId = page.getStoryId();
            String prompt = page.getScene();
            image.request = ImageGenerationRequest.page(prompt);
        }

        image = getResult(image);

        // 3. Guardamos la imagen
        String path = storage.save(image.storyId, kind, id, image.result);

        if(kind == KindImage.ACTOR)
            actorUseCase.setImagePath(id, path);
        else
            pageUseCase.setImagePath(id, path);
        
        // 5. Debolvemos la respuesta
        return image.result;
    }
    
    private ImageResult getResult(ImageResult result) {
        result.result = generator.generateImage(result.request);
        return result;
    }

    class ImageResult { long storyId; ImageGenerationRequest request; byte[] result; }
}
