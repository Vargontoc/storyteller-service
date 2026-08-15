package es.vargontoc.storyteller.application.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import es.vargontoc.storyteller.application.ports.in.generator.TopicGenerator;
import es.vargontoc.storyteller.application.ports.in.persistence.TopicUseCase;
import es.vargontoc.storyteller.application.ports.out.external.OllamaPort;
import es.vargontoc.storyteller.application.ports.out.persistence.TopicRepository;
import es.vargontoc.storyteller.domain.command.TopicGenerateCommand;
import es.vargontoc.storyteller.domain.model.Topic;
import es.vargontoc.storyteller.shared.exceptions.AppException;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class TopicService implements TopicGenerator, TopicUseCase {

    private static final double SIMILARITY_THRESHOLD = 0.87;
    private static final int MAX_ATTEMPTS = 3;

    private final ChatClient client;
    private final OllamaPort ollama;
    private final TopicRepository repository;
    private final VectorStore  vector;


    public TopicService(ChatClient client, OllamaPort ollama, TopicRepository repository, VectorStore vector) {
        this.client = client;
        this.ollama = ollama;
        this.repository = repository;
        this.vector = vector;
    }

    @Override
    public Topic generate(TopicGenerateCommand cmd) {
       // 1. Check if model is active
        if(!ollama.isAvailable("storyteller-topics"))
            throw new AppException("El agente encargado de esta operación no está disponible", HttpStatus.BAD_REQUEST);

        for(int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++ ){
            Topic candidate = generateCandidate();

            String content = candidate.getType() + " - " + candidate.getDescription();
            if(!isSemanticDuplicate(content)){
                var stored = repository.createTopic(candidate);
                indexTopic(stored, content);
                return stored;
            }
        }

        throw new AppException("No se pudo generar un topic novedoso tras " + MAX_ATTEMPTS + " intentos", HttpStatus.CONFLICT);
    }
  private Topic generateCandidate() {
        try {
            return client.prompt()
                .user("""
                    Inventa un nuevo tema (type y description) para un cuento
                    infantil de 3 a 4 años. type: máx 30 caracteres.
                    description: máx 100 caracteres.
                    """)
                .call()
                .entity(Topic.class); // format JSON schema forzado por request
        } catch (Exception e) {
            throw new AppException(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


    private boolean isSemanticDuplicate(String content) {
        List<Document> similar = vector.similaritySearch(
            SearchRequest.builder()
                .query(content)
                .topK(1)
                .similarityThreshold(SIMILARITY_THRESHOLD)
                .build());
        return !similar.isEmpty();
    }
    @Override
    public List<Topic> getTopics() {
        return repository.getTopics();
    }
    
    private void indexTopic(Topic topic, String content) {
        Document doc = new Document(
            UUID.randomUUID().toString(),
            content,
            Map.of(
                "topicId", topic.getId(),
                "type", topic.getType()
            ));
        vector.add(List.of(doc));
    }
    
}
