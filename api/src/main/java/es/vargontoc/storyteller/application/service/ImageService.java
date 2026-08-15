package es.vargontoc.storyteller.application.service;

import org.springframework.stereotype.Service;

import es.vargontoc.storyteller.application.ports.in.generator.ImageGeneration;
import es.vargontoc.storyteller.application.ports.in.persistence.ActorUseCase;
import es.vargontoc.storyteller.application.ports.out.external.ImageGeneratorPort;
import es.vargontoc.storyteller.domain.model.Actor;
import es.vargontoc.storyteller.domain.request.ImageGenerationRequest;
import es.vargontoc.storyteller.infrastructure.config.ComfyUIProperties;
import es.vargontoc.storyteller.infrastructure.storage.CharacterImageStorage;

@Service
public class ImageService implements ImageGeneration {

    private final ActorUseCase actorUseCase;
    private final ImageGeneratorPort generator;
    private final ComfyUIProperties properties;
    private final CharacterImageStorage storage;


    public ImageService(ActorUseCase actorUseCase, ImageGeneratorPort generator, ComfyUIProperties properties,
            CharacterImageStorage storage) {
        this.actorUseCase = actorUseCase;
        this.generator = generator;
        this.properties = properties;
        this.storage = storage;
    }


    @Override
    public byte[] generateImageCharacter(long id) {
        // 1. Cargamos el personaje
        Actor character = actorUseCase.getActor(id);

        // 2. Generamos la imagen
        String prompt = properties.stylePrefix() + properties.characterFramingPrompt() +
            properties.styleTriggerWord() == null || properties.styleTriggerWord().isBlank() ? "" : ", " +
            properties.styleTriggerWord() +
            ", " + character.getVisualDescription();

        byte[] img = generator.generateImage(new ImageGenerationRequest(prompt, null));

        // 3. Guardamos la imagen
        String path = storage.save(id, img);

        // 4. Guardar los bytes y el path de rerencia

        // 5. Debolvemos la respuesta
        return img;
    }
    
}
