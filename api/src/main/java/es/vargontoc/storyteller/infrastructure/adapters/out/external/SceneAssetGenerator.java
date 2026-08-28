package es.vargontoc.storyteller.infrastructure.adapters.out.external;

import java.nio.file.Path;
import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import es.vargontoc.storyteller.application.ports.in.WebsocketUseCase;
import es.vargontoc.storyteller.application.ports.out.ResourceStorage;
import es.vargontoc.storyteller.application.ports.out.external.ComfyUIPort;
import es.vargontoc.storyteller.application.ports.out.external.ImageGeneratorPort;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryPageRepository;
import es.vargontoc.storyteller.application.utils.ReferenceCharacterSelector;
import es.vargontoc.storyteller.domain.enums.KindImage;
import es.vargontoc.storyteller.domain.events.WebsocketNotificationEvent;
import es.vargontoc.storyteller.domain.model.Actor;
import es.vargontoc.storyteller.domain.model.ActorAction;
import es.vargontoc.storyteller.domain.model.SceneComposition;
import es.vargontoc.storyteller.domain.request.ImageGenerationRequest;
import es.vargontoc.storyteller.domain.request.SceneRequest;
import jakarta.transaction.Transactional;

@Transactional
@Component
public class SceneAssetGenerator {
    
    private final ComfyUIPort imagePort;
    private final StoryPageRepository repository;
    private final ResourceStorage storage;
    private final ImageGeneratorPort generator;
    private final WebsocketUseCase websocket;

    
    public SceneAssetGenerator(ComfyUIPort imagePort, StoryPageRepository repository, ResourceStorage storage,
            ImageGeneratorPort generator, WebsocketUseCase websocket) {
        this.imagePort = imagePort;
        this.repository = repository;
        this.storage = storage;
        this.generator = generator;
        this.websocket = websocket;
    }


    @Async
    public void generateImageScene(Long id) {
        if(!imagePort.isAvailableService())
        {
            websocket.sendNotification(WebsocketNotificationEvent.error("El generador de imagenes no está disponible"));
            return;
        }

        var page = repository.getPage(id);
        ImageGenerationRequest request = null;
        SceneComposition composition = page.getComposition();

        List<Actor> actorsInScene = repository.getSceneActors(page.getStoryId(), composition.actors().stream().map(ActorAction::name).toList(), page.getPage() == 0);
        List<Path> references = ReferenceCharacterSelector.selectReferenceImages(actorsInScene, page.getPage() == 0 ? 1 : 2);
        if(actorsInScene.stream().anyMatch(x -> x.getImage() == null || x.getImage().isBlank()))
        {
            websocket.sendNotification(WebsocketNotificationEvent.error("Hay algun personaje sin generar su imagen. Generelo primero"));
            return;
        }

        if(page.getPage() == 0) {
            request = ImageGenerationRequest.cover(composition.background(), references);
        }else {

            if(actorsInScene.size() > 1) {
                String visual1 = actorsInScene.get(0).getMetadata().translate();
                String action1 = composition.actors().stream().filter(x -> actorsInScene.get(0).getName().equals(x.name())).findFirst().get().action();
                Path path1 = Path.of(actorsInScene.get(0).getImage());

                String visual2 = actorsInScene.get(1).getMetadata().translate();
                String action2 = composition.actors().stream().filter(x -> actorsInScene.get(1).getName().equals(x.name())).findFirst().get().action();
                Path path2 = Path.of(actorsInScene.get(1).getImage());


                request = ImageGenerationRequest.page(composition.background(), references,
                    new SceneRequest(composition.background(), visual1, action1, path1, visual2, action2, path2));
            }else {
                request = ImageGenerationRequest.cover(composition.background(), references);
            }
        }


        try {
            
            String path = storage.saveImage(page.getStoryId(), page.getPage() == 0 ? KindImage.COVER : KindImage.PAGE, id, generator.generateImage(request));
            repository.setImagePath(id, path);

            websocket.sendNotification(WebsocketNotificationEvent.success("Escena generada con exito"));

        }catch(Exception e) {
            websocket.sendNotification(WebsocketNotificationEvent.error("Error en la generación de escena. Intentelo más tarde"));
        }
    }
}
