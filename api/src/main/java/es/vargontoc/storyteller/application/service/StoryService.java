package es.vargontoc.storyteller.application.service;

import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import es.vargontoc.storyteller.application.ports.in.generator.StoryGeneration;
import es.vargontoc.storyteller.application.ports.in.persistence.StoryUseCase;
import es.vargontoc.storyteller.application.ports.out.external.OllamaPort;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryReviewRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.TopicRepository;
import es.vargontoc.storyteller.domain.command.StoryGenerateCommand;
import es.vargontoc.storyteller.domain.command.StoryReviewCommand;
import es.vargontoc.storyteller.domain.model.RevisionStatus;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.model.StoryReview;
import es.vargontoc.storyteller.domain.model.Topic;
import es.vargontoc.storyteller.domain.response.StoryAgentResult;
import es.vargontoc.storyteller.domain.response.StoryReviewAgentResult;
import es.vargontoc.storyteller.infrastructure.adapters.in.rest.dto.ConfirmReviewRequestDto;
import es.vargontoc.storyteller.shared.Constants;
import es.vargontoc.storyteller.shared.exceptions.AppException;
import es.vargontoc.storyteller.shared.exceptions.ResourceNotFoundException;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class StoryService implements StoryGeneration, StoryUseCase   {

    @Value("classpath:/prompts/new_script.st")
    private Resource scriptResource;
    @Value("classpath:/prompts/review_script.st")
    private Resource reviewScriptResource;

    private final OllamaPort ollama;
    private final String model;
    private final ChatClient client;


    private final StoryRepository repository;
    private final StoryReviewRepository reviewRepository;
    private final TopicRepository topicRepository;

    public StoryService(StoryRepository repository,
            TopicRepository topicRepository,
            StoryReviewRepository reviewRepository,
            OllamaPort ollama,
            @Qualifier(Constants.BeanNames.AGENT_DIRECTOR_MODEL) String model,
            @Qualifier(Constants.BeanNames.AGENT_DIRECTOR) ChatClient client){

        this.repository = repository;
        this.topicRepository = topicRepository;
        this.reviewRepository = reviewRepository;
        this.ollama = ollama;
        this.model = model;
        this.client  = client;
    }

    @Override
    public Story getStory(Long storyId) {
        return repository.getStory(storyId);
    }

    @Override
    public Story generate(StoryGenerateCommand cmd) {
        
        if(!ollama.isAvailable(model))
            throw new AppException("El agente encargado de esta operación no está disponible", HttpStatus.BAD_REQUEST);
        
        Topic topic = topicRepository.getTopics().stream().filter(x -> x.getId().equals(cmd.topicId())).findFirst().orElseThrow(() -> {
            throw new ResourceNotFoundException("Topic requerido no encontrado");
        });

        StoryAgentResult result = client.prompt().user(u -> u.text(scriptResource)
            .param("topic", topic.getType())
            .param("description", topic.getDescription())
            .param("pages", cmd.size().getPages()))
        .call().entity(StoryAgentResult.class);

        return repository.create(result, cmd.size(), cmd.topicId());
    }

    @Override
    public StoryReview getReview(Long entityId) {
        repository.getStory(entityId);
        return reviewRepository.getPendingReview(entityId);
    }

    @Override
    public StoryReview review(StoryReviewCommand review) {
        // 1. Comprobar que el agente esta en el servidor Ollama
        if(!ollama.isAvailable(model))
            throw new AppException("El agente encargado de esta operación no está disponible", HttpStatus.BAD_REQUEST);

        // 2. Obtenemos la story existente
        Story current = repository.getStory(review.storyId());
        
        // 3. Obtenemos el tema de la story
        Topic t = topicRepository.getTopicByStoryId(review.storyId());

        // 4. Descartamos review prendiente
        reviewRepository.changeStatus(review.storyId(), RevisionStatus.DISCARDED);

        // 3. Llamamos al agente
        StoryReviewAgentResult result = client.prompt().user(u -> u.text(reviewScriptResource)
            .param("topic", readTopic(t))
            .param("title", current.getTitle())
            .param("synopsis", current.getSummary())
            .param("pages", current.getSize().getPages())
            .param("hint", review.hint())
            .param("characters", describeCharacters(current)))
        .call().entity(StoryReviewAgentResult.class);

        
        // 4. Devolvemos el resultado mapeado
        return reviewRepository.createStoryReview(result, review.storyId(), review.hint());
    }

    @Override
    public Story confirmReview(ConfirmReviewRequestDto request) {
        // 1. Obtenemos la story por id
        Story current = repository.getStory(request.entityId());
        
        // 2. Obtenemos la revision pendiente
        StoryReview review = getReview(request.entityId());
        if(review == null)
            throw new AppException("No hay una review pendiente para la story: " + request.entityId(), HttpStatus.CONFLICT);
        
        // 3. Si el usuario descarta, descartamos la review
        if(request.status() == RevisionStatus.DISCARDED)
            reviewRepository.changeStatus(request.entityId(), RevisionStatus.DISCARDED);
        else if(request.status()  == RevisionStatus.CONFIRMED)
        {
            reviewRepository.changeStatus(request.entityId(), RevisionStatus.CONFIRMED);
            
            // 4. Guardar el cambio
            applyChanges(current, review);
            return repository.update(current);
        }

        return current;
    }

    private void applyChanges(Story story, StoryReview review) {
        story.setTitle(review.getPreviewStory().getTitle());
        story.setSummary(review.getPreviewStory().getSummary());
        story.setCharacters(review.getPreviewStory().getCharacters());
    }

    
    private String readTopic(Topic t) {
        return t + " / " + t.getDescription();
    }

    private String describeCharacters(Story story) {
        return story.getCharacters().stream()
            .map(c -> c.getName() + " ( " + c.getNarrativeDescription() + " / " + c.getVisualDescription() + " )")
            .collect(Collectors.joining("; "));
    }

    
}
