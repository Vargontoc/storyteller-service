package es.vargontoc.storyteller.service;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import es.vargontoc.storyteller.domain.Topic;
import es.vargontoc.storyteller.infrastructure.tools.TopicTools;
import es.vargontoc.storyteller.ports.in.TopicUseCase;
import es.vargontoc.storyteller.ports.out.OllamaPort;
import es.vargontoc.storyteller.ports.out.TopicRepository;
import es.vargontoc.storyteller.shared.exceptions.AppException;

@Service
public class TopicService implements TopicUseCase {

    private final ChatClient client;
    private final OllamaPort ollama;
    private final TopicRepository repository;
    private final TopicTools topicTools;

    public TopicService(@Qualifier("topics-agent") ChatClient client, OllamaPort ollama, TopicRepository repository, TopicTools topicTools){
        this.ollama = ollama;
        this.client = client;
        this.repository = repository;
        this.topicTools = topicTools;
    }

    @Override
    public Topic generate() {
        if(!ollama.isAvailable("storyteller-topics"))
            throw new AppException("El agente encargado de esta operación no está disponible", HttpStatus.BAD_REQUEST);

        try {
            return client.prompt()
                .user("Genera un nuevo topic y devuelve el topic creado en el formato solicitado")
                .call()
                .entity(Topic.class);
        }catch(Exception e) {
            throw new AppException(e.getMessage(), HttpStatus.BAD_REQUEST);
        } finally {
            topicTools.resetCreationGuard();
        }
    }

    @Override
    public List<Topic> getTopics() {
        return repository.getTopics();
    }
    
}
