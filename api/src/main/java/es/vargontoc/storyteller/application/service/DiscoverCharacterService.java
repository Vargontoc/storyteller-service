package es.vargontoc.storyteller.application.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import es.vargontoc.storyteller.application.ports.in.WebsocketUseCase;
import es.vargontoc.storyteller.application.ports.out.external.OllamaPort;
import es.vargontoc.storyteller.application.ports.out.persistence.CharacterRepository;
import es.vargontoc.storyteller.domain.events.WebsocketNotificationEvent;
import es.vargontoc.storyteller.domain.model.Actor;
import es.vargontoc.storyteller.domain.model.ActorAction;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.response.CharacterAgentResult;
import es.vargontoc.storyteller.shared.Constants;
import jakarta.transaction.Transactional;

@Component
@Transactional
public class DiscoverCharacterService {
    
    @Value("classpath:/prompts/discover_character.st")
    private Resource discoverCharacterResource;

    private final OllamaPort ollama;
    private final String model;
    private final ChatClient client;
    private final CharacterRepository characterRepository;
    private final WebsocketUseCase websocket;


    
    public DiscoverCharacterService(
            OllamaPort ollama,
            @Qualifier(Constants.BeanNames.AGENT_DIRECTOR_MODEL)String model,
            @Qualifier(Constants.BeanNames.AGENT_DIRECTOR) ChatClient client,
            CharacterRepository characterRepository,
            WebsocketUseCase websocket) {
        this.ollama = ollama;
        this.model = model;
        this.client = client;
        this.characterRepository = characterRepository;
        this.websocket = websocket;
    }


    @Async
    public void discoverCharacters(List<ActorAction> actors, Story story, String scene){
        var discovered = actors.stream().filter(x -> !story.getCharacters().stream().anyMatch(y -> y.getName().equalsIgnoreCase(x.name()))).toList();
        if(!discovered.isEmpty())
            websocket.sendNotification(WebsocketNotificationEvent.info("Generando nuevos personajes"));

        if(!ollama.isAvailable(model)) {
            websocket.sendNotification(WebsocketNotificationEvent.error("El agente director está detenido"));
            return;
        }

        discovered.forEach(d -> discoverCharacter(d,  story, scene));

        websocket.sendNotification(WebsocketNotificationEvent.success("Se han generado los personajes con exito"));
    }


    private void discoverCharacter(ActorAction actor, Story story, String scene){
        websocket.sendNotification(WebsocketNotificationEvent.info("Generando nuevo personaje"));
        CharacterAgentResult result = client
            .prompt()
            .user(u -> u.text(discoverCharacterResource)
                .param("synopsis", story.getSynopsis())
                .param("name", actor.name())
                .param("appear", scene)
                .param("actors", readActors(story.getCharacters())))
            .call()
            .entity(CharacterAgentResult.class);

        characterRepository.createActor(story.getId(), result);
        websocket.sendNotification(WebsocketNotificationEvent.info("Personaje " + actor.name() + " generado"));
    }

    private String readActors(List<Actor> characters) {
        return characters.stream()
            .map(c ->"- " + c.getName() + " ( " + c.getNarrativeDescription() + " / " + c.getVisualDescription() + " )")
            .collect(Collectors.joining(";\n"));
    }
}
