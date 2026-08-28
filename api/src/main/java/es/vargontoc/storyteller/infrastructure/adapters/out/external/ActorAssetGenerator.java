package es.vargontoc.storyteller.infrastructure.adapters.out.external;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import es.vargontoc.storyteller.application.ports.in.WebsocketUseCase;
import es.vargontoc.storyteller.application.ports.out.ResourceStorage;
import es.vargontoc.storyteller.application.ports.out.external.ComfyUIPort;
import es.vargontoc.storyteller.application.ports.out.external.ImageGeneratorPort;
import es.vargontoc.storyteller.application.ports.out.persistence.CharacterRepository;
import es.vargontoc.storyteller.domain.enums.KindImage;
import es.vargontoc.storyteller.domain.events.WebsocketNotificationEvent;
import es.vargontoc.storyteller.domain.request.ImageGenerationRequest;
import jakarta.transaction.Transactional;

@Transactional
@Component
public class ActorAssetGenerator {
    
    private final ComfyUIPort imagePort;
    private final CharacterRepository repository;
    private final ResourceStorage storage;
    private final ImageGeneratorPort generator;
    private final WebsocketUseCase websocket;


    
    public ActorAssetGenerator(ComfyUIPort port, CharacterRepository repository, ResourceStorage storage, ImageGeneratorPort generator,
            WebsocketUseCase websocket) {
        this.imagePort = port;
        this.repository = repository;
        this.storage = storage;
        this.generator = generator;
        this.websocket = websocket;
    }



    @Async
    public void generateImageActor(Long id) 
    {
        if(!imagePort.isAvailableService())
        {
            websocket.sendNotification(WebsocketNotificationEvent.error("El generador de imagenes no está disponible"));
            return;
        }
        var actor = repository.getActor(id);
        var data = actor.getMetadata();

        websocket.sendNotification(WebsocketNotificationEvent.info("Generando imagen personaje: " + actor.getName()));
        ImageGenerationRequest request = ImageGenerationRequest.actor(data.translate(), data.attributes());
        try {
            
            String path = storage.saveImage(actor.getStoryId(), KindImage.ACTOR, id, generator.generateImage(request));
            repository.setImagePath(id, path);

            websocket.sendNotification(WebsocketNotificationEvent.success("Imagen generada con exito"));

        }catch(Exception e) {
            websocket.sendNotification(WebsocketNotificationEvent.error("Error en la generación de personaje. Intentelo más tarde"));
        }
    }
}
