package es.vargontoc.storyteller.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import es.vargontoc.storyteller.application.ports.in.generator.ActorGeneration;
import es.vargontoc.storyteller.application.ports.in.persistence.ActorUseCase;
import es.vargontoc.storyteller.application.ports.out.external.OllamaPort;
import es.vargontoc.storyteller.application.ports.out.persistence.CharacterRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.CharacterReviewRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryRepository;
import es.vargontoc.storyteller.domain.command.ActorReviewCommand;
import es.vargontoc.storyteller.domain.enums.RevisionStatus;
import es.vargontoc.storyteller.domain.model.Actor;
import es.vargontoc.storyteller.domain.model.ActorReview;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.response.CharacterReviewAgentResult;
import es.vargontoc.storyteller.infrastructure.adapters.in.rest.dto.ConfirmReviewRequestDto;
import es.vargontoc.storyteller.shared.Constants;
import es.vargontoc.storyteller.shared.exceptions.AppException;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class CharacterService implements ActorGeneration, ActorUseCase {
    
    @Value("classpath:/prompts/review_character.st")
    private Resource reviewCharacterResource;

    private final OllamaPort ollama;
    private final String model;
    private final ChatClient client;
    
    private final CharacterRepository repository;
    private final CharacterReviewRepository reviewRepository;
    private final StoryRepository storyRepository;
    
    

    public CharacterService( 
            OllamaPort ollama,
            @Qualifier(Constants.BeanNames.AGENT_DIRECTOR_MODEL) String model,
            @Qualifier(Constants.BeanNames.AGENT_DIRECTOR) ChatClient client,
            CharacterRepository repository,
            CharacterReviewRepository reviewRepository, 
            StoryRepository storyRepository) {
        this.ollama = ollama;
        this.model = model;
        this.client = client;
        this.repository = repository;
        this.reviewRepository = reviewRepository;
        this.storyRepository = storyRepository;
    }

    @Override
    public Actor getActor(Long id) {
        return repository.getActor(id);
    }

    @Override
    public List<Actor> getActors(Long storyId) {
        return new ArrayList<>();
    }
    @Override
    public ActorReview review(ActorReviewCommand review) {
                // 1. Comprobar que el agente esta en el servidor Ollama
        if(!ollama.isAvailable(model))
            throw new AppException("El agente encargado de esta operación no está disponible", HttpStatus.BAD_REQUEST);
        
        Story currentStory = storyRepository.getStory(review.storyId());
        

        // 2. Obtenemos el protagonista afectado
        Actor current = getActor(review.id());

        // 3. Obtenemos la historia actual

        // 4. Descartamos reviews pendientes
        reviewRepository.changeStatus(current.getId(), RevisionStatus.DISCARDED);

        // 5. Llamamos al agente
        CharacterReviewAgentResult result = client.prompt().user(u -> u.text(reviewCharacterResource)
            .param("synopsis", currentStory.getSummary())
            .param("other", readOtherCharacters(currentStory.getCharacters(), review.id()))
            .param("narrativeDescription", current.getNarrativeDescription())
            .param("visualDescription", current.getVisualDescription())
            .param("target", review.target().toString())
            .param("hint", review.hint())
            .param("visualAttributes", current.getMetadata().attributes().stream().collect(Collectors.joining(";\n"))))
        .call().entity(CharacterReviewAgentResult.class);

        // 6. Persistimos y devolvemos resultado
        return reviewRepository.createReview(result, review.id(), review.target(), review.hint());
    }
    
    

    private String readOtherCharacters(List<Actor> characters, long id) {
        return characters.stream().filter(x -> x.getId() != id)
            .map(c -> "( " + c.getNarrativeDescription() +" / " +  c.getVisualDescription()  + " )")
            .collect(Collectors.joining(";"));
    }

    @Override
    public ActorReview getReview(Long entityId) {
        return reviewRepository.getPendingReview(entityId);
    }


    @Override
    public Actor confirmReview(ConfirmReviewRequestDto request) {
        // 1. Obtenemos personaje actual en bbdd
        Actor current = getActor(request.entityId());

        // 2. Obtenemos la revision pendiente
        ActorReview review = reviewRepository.getPendingReview(request.entityId());
        if(review == null)
            throw new AppException("No hay una review pendiente para el character: " + request.entityId(), HttpStatus.CONFLICT);

        if(request.status() == RevisionStatus.DISCARDED)
            reviewRepository.changeStatus(request.entityId(), RevisionStatus.DISCARDED);
        else if(request.status() == RevisionStatus.CONFIRMED) {
            reviewRepository.changeStatus(request.entityId(), RevisionStatus.CONFIRMED);

            //3. Persistir los camios
            applyChanges(current, review);
            return repository.update(current);
        }


        return current;
    }

    private void applyChanges(Actor current, ActorReview review) {
        current.setNarrativeDescription(review.getNarrativeDescription());
        current.setVisualDescription(review.getVisualDescription());
    }

}
