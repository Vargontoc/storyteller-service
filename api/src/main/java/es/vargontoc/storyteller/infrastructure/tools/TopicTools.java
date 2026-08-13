package es.vargontoc.storyteller.infrastructure.tools;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import es.vargontoc.storyteller.domain.Topic;
import es.vargontoc.storyteller.ports.out.TopicRepository;

@Component
public class TopicTools {
    
    private final TopicRepository repository;
    public TopicTools(TopicRepository repository) {
        this.repository = repository;
    }

    @Tool(name = "get_topics", description = "Obtiene los temas infantiles ya registrados")
    List<Topic> getTopics() {
        return repository.getTopics();
    }

    private final ThreadLocal<Topic> creationGuard = new ThreadLocal<>();

    @Tool(name = "create_topic", description = "Crea un nuevo tema infantil segun las reglas de negocio")
    Topic getTopic(@ToolParam(description = "tipo de tema") String type, @ToolParam(description = "Descripcion del tema") String description) {
        Topic alreadyCreated = creationGuard.get();
        if (alreadyCreated != null) {
            return alreadyCreated;
        }

        Topic t = new Topic();
        t.setType(type);
        t.setDescription(description);
        Topic created = repository.createTopic(t);
        creationGuard.set(created);
        return created;
    }

    public void resetCreationGuard() {
        creationGuard.remove();
    }
}
