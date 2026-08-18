package es.vargontoc.storyteller.application.service;

import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import es.vargontoc.storyteller.application.ports.in.generator.AudioGeneration;
import es.vargontoc.storyteller.application.ports.in.generator.ImageGeneration;
import es.vargontoc.storyteller.application.ports.out.external.AudioGeneratorPort;
import es.vargontoc.storyteller.application.ports.out.external.ImageGeneratorPort;
import es.vargontoc.storyteller.application.ports.out.persistence.CharacterRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryPageRepository;
import es.vargontoc.storyteller.domain.command.AudioGenerateCommand;
import es.vargontoc.storyteller.domain.enums.KindImage;
import es.vargontoc.storyteller.domain.enums.VoiceTonePreset;
import es.vargontoc.storyteller.domain.model.AudioToneParams;
import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.domain.request.AudioGenerationRequest;
import es.vargontoc.storyteller.domain.request.ImageGenerationRequest;
import es.vargontoc.storyteller.infrastructure.config.ChatterboxProperties;
import es.vargontoc.storyteller.infrastructure.storage.ResourceStorageAdapter;
import es.vargontoc.storyteller.shared.exceptions.AppException;

@Service
public class AssetsGenerationService implements ImageGeneration, AudioGeneration {

    private final CharacterRepository actorUseCase;
    private final StoryPageRepository pageUseCase;

    private final ImageGeneratorPort generator;
    private final AudioGeneratorPort audioGenerator;
    private final ResourceStorageAdapter storage;

    private final String defaultVoice;


    public AssetsGenerationService(ChatterboxProperties chatterbox, AudioGeneratorPort audioPort, CharacterRepository actorUseCase, StoryPageRepository pageUseCase, ImageGeneratorPort generator,
            ResourceStorageAdapter storage) {
        this.actorUseCase = actorUseCase;
        this.pageUseCase = pageUseCase;
        this.generator = generator;
        this.storage = storage;
        defaultVoice = chatterbox.defaultVoiceName();
        audioGenerator = audioPort;

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
        String path = storage.saveImage(image.storyId, kind, id, image.result);

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

    @Override
    public byte[] generateAudio(AudioGenerateCommand cmd) {
        // 1. Obtenemos la pagina de la peticion
        StoryPage page = pageUseCase.getPage(cmd.pageId());

        // 2. Obtenemos el nombre
        String voiceName = cmd.voiceName() != null && !cmd.voiceName().isBlank() ? cmd.voiceName() : defaultVoice;

        // 3. Establecemos parametros
        AudioToneParams params = cmd.preset() == VoiceTonePreset.CUSTOM ? Objects.requireNonNull(cmd.customParams(), "customParams requerido con preset CUSTOM") : cmd.preset().toParams();

        // 4. Generacion del audio
        byte[] bytes = audioGenerator.generateAudio(new AudioGenerationRequest(
            page.getPage() != 0 ? page.getText() : page.getCoverText(),
            voiceName,
            params.exageration(),
            params.cfgWeight(),
            params.temperature()
        ));

        // 5. Guardamos el fichero audio
        String path = storage.saveAudio(page.getStoryId(), page.getId(), bytes);

        // 5. Persistir el asset
        pageUseCase.setAudioPath(page.getId(), path);

        return bytes;
    }
}
