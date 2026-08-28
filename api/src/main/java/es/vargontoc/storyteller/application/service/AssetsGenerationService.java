package es.vargontoc.storyteller.application.service;


import java.util.List;
import org.springframework.stereotype.Service;

import es.vargontoc.storyteller.application.ports.in.generator.AudioGeneration;
import es.vargontoc.storyteller.application.ports.in.generator.ImageGeneration;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryPageRepository;
import es.vargontoc.storyteller.domain.command.AudioGenerateCommand;
import es.vargontoc.storyteller.domain.enums.KindImage;
import es.vargontoc.storyteller.domain.model.AudioPage;
import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.infrastructure.adapters.out.external.ActorAssetGenerator;
import es.vargontoc.storyteller.infrastructure.adapters.out.external.PageAudioAssetGenerator;
import es.vargontoc.storyteller.infrastructure.adapters.out.external.SceneAssetGenerator;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class AssetsGenerationService implements ImageGeneration, AudioGeneration {


    private final StoryPageRepository pageUseCase;
    private final ActorAssetGenerator actorGenerator;
    private final SceneAssetGenerator sceneGenerator;
    private final PageAudioAssetGenerator sceneAudioGenerator;
    


    public AssetsGenerationService(StoryPageRepository pageUseCase, ActorAssetGenerator actorGenerator,
            SceneAssetGenerator sceneGenerator, PageAudioAssetGenerator sceneAudioGenerator) {
        this.pageUseCase = pageUseCase;
        this.actorGenerator = actorGenerator;
        this.sceneGenerator = sceneGenerator;
        this.sceneAudioGenerator = sceneAudioGenerator;
    }

    @Override
    public byte[] generateImage(KindImage kind, long id) {

        if(kind == KindImage.ACTOR) {
            actorGenerator.generateImageActor(id);
        }
        else{
            sceneGenerator.generateImageScene(id);
        }
        return new byte[]{};
    }
    


    @Override
    public byte[] generateAudio(AudioGenerateCommand cmd) {
        // 1. Obtenemos la pagina de la peticion
        StoryPage page = pageUseCase.getPage(cmd.pageId());

        sceneAudioGenerator.generatePageAudios(page.getStoryId(), List.of(
            new AudioPage(page.getPage(), page.getText(), cmd.preset(), null)
        ));

        return new byte[]{};
    }
}
