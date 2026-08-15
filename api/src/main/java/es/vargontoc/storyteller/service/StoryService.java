package es.vargontoc.storyteller.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import es.vargontoc.storyteller.domain.CharacterModel;
import es.vargontoc.storyteller.domain.CharacterReview;
import es.vargontoc.storyteller.domain.CharacterReviewAgentResult;
import es.vargontoc.storyteller.domain.RevisionStatus;
import es.vargontoc.storyteller.domain.Story;
import es.vargontoc.storyteller.domain.StoryAgentResult;
import es.vargontoc.storyteller.domain.StoryReview;
import es.vargontoc.storyteller.domain.StoryReviewAgentResult;
import es.vargontoc.storyteller.domain.Topic;
import es.vargontoc.storyteller.infrastructure.dto.ConfirmReviewRequestDto;
import es.vargontoc.storyteller.infrastructure.dto.ReviewCharacterRequestDto;
import es.vargontoc.storyteller.infrastructure.dto.StoryRequestDto;
import es.vargontoc.storyteller.ports.in.StoryUseCase;
import es.vargontoc.storyteller.ports.out.CharacterRepository;
import es.vargontoc.storyteller.ports.out.CharacterReviewRepository;
import es.vargontoc.storyteller.ports.out.OllamaPort;
import es.vargontoc.storyteller.ports.out.StoryRepository;
import es.vargontoc.storyteller.ports.out.StoryReviewRepository;
import es.vargontoc.storyteller.ports.out.TopicRepository;
import es.vargontoc.storyteller.shared.Constants;
import es.vargontoc.storyteller.shared.exceptions.AppException;
import es.vargontoc.storyteller.shared.exceptions.ResourceNotFoundException;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class StoryService implements StoryUseCase {

    @Value("classpath:/templates/new_script.st")
    private Resource scriptResource;

    @Value("classpath:/templates/review_script.st")
    private Resource reviewScriptResource;

    @Value("classpath:/templates/review_character.st")
    private Resource reviewCharacterResource;

    private final TopicRepository topicRepository;
    private final StoryRepository storyRepository;
    private final CharacterRepository characterRepository;

    private final StoryReviewRepository storyReviewRepository;
    private final CharacterReviewRepository characterReviewRepository;

    private final OllamaPort ollama;
    private final String directorMopdel;
    private final ChatClient agent;

    public StoryService(TopicRepository topicRepository,
        OllamaPort ollama,
        @Qualifier(Constants.BeanNames.AGENT_DIRECTOR_MODEL) String directorModel,
        @Qualifier(Constants.BeanNames.AGENT_DIRECTOR) ChatClient agent,
        StoryRepository storyRepository,
        CharacterRepository characterRepository,
        CharacterReviewRepository characterReviewRepository,
        StoryReviewRepository storyReviewRepository) {
        this.topicRepository = topicRepository;
        this.ollama = ollama;
        this.directorMopdel = directorModel;
        this.agent = agent;
        this.storyRepository = storyRepository;
        this.storyReviewRepository = storyReviewRepository;
        this.characterRepository = characterRepository;
        this.characterReviewRepository = characterReviewRepository;
    }


    @Override
    public Story generateStory(StoryRequestDto request) {
        if(!ollama.isAvailable(directorMopdel))
            throw new AppException("El agente encargado de esta operación no está disponible", HttpStatus.BAD_REQUEST);
        
        Topic topic = topicRepository.getTopics().stream().filter(x -> x.getId().equals(request.topicId())).findFirst().orElseThrow(() -> {
            throw new ResourceNotFoundException("Topic requerido no encontrado");
        });

        StoryAgentResult result = agent.prompt().user(u -> u.text(scriptResource)
            .param("topic", topic.getType())
            .param("description", topic.getDescription())
            .param("pages", request.size().getPages()))
        .call().entity(StoryAgentResult.class);

        return storyRepository.create(result, request.size(), request.topicId());
    }

    
    @Override
    public Story getStory(Long storyId) {
        return storyRepository.getStory(storyId);
    }

    @Override
    public StoryReview reviewStory(Long storyId, String hint) {
        // 1. Comprobar que el agente esta en el servidor Ollama
        if(!ollama.isAvailable(directorMopdel))
            throw new AppException("El agente encargado de esta operación no está disponible", HttpStatus.BAD_REQUEST);

        // 2. Obtenemos la story existente
        Story current = storyRepository.getStory(storyId);
        
        // 3. Obtenemos el tema de la story
        Topic t = topicRepository.getTopicByStoryId(storyId);

        // 4. Descartamos review prendiente
        storyReviewRepository.changeStatus(storyId, RevisionStatus.DISCARDED);

        // 3. Llamamos al agente
        StoryReviewAgentResult result = agent.prompt().user(u -> u.text(reviewScriptResource)
            .param("topic", readTopic(t))
            .param("title", current.getTitle())
            .param("synopsis", current.getSummary())
            .param("pages", current.getSize().getPages())
            .param("hint", hint)
            .param("characters", describeCharacters(current)))
        .call().entity(StoryReviewAgentResult.class);

        
        // 4. Devolvemos el resultado mapeado
        return storyReviewRepository.createStoryReview(result, storyId, hint);
    }

    @Override
    public StoryReview getPendingReview(Long storyId) {
        storyRepository.getStory(storyId);
        return storyReviewRepository.getPendingReview(storyId);
    }

    @Override
    public Story confirmReviewStory(Long storyId, ConfirmReviewRequestDto request) {
        // 1. Obtenemos la story por id
        Story current = storyRepository.getStory(storyId);
        
        // 2. Obtenemos la revision pendiente
        StoryReview review = getPendingReview(storyId);
        if(review == null)
            throw new AppException("No hay una review pendiente para la story: " + storyId, HttpStatus.CONFLICT);
        
        // 3. Si el usuario descarta, descartamos la review
        if(request.status() == RevisionStatus.DISCARDED)
            storyReviewRepository.changeStatus(storyId, RevisionStatus.DISCARDED);
        else if(request.status()  == RevisionStatus.CONFIRMED)
        {
            storyReviewRepository.changeStatus(storyId, RevisionStatus.CONFIRMED);
            
            // 4. Guardar el cambio
            applyChanges(current, review);
            return storyRepository.update(current);
        }

        return current;
    }

    @Override
    public CharacterReview reviewCharacter(Long storyId, Long characterId, ReviewCharacterRequestDto request) {
        // 1. Comprobar que el agente esta en el servidor Ollama
        if(!ollama.isAvailable(directorMopdel))
            throw new AppException("El agente encargado de esta operación no está disponible", HttpStatus.BAD_REQUEST);
        
        // 2. Obtenemos el protagonista afectado
        CharacterModel current = getCharacter(storyId, characterId);

        // 3. Obtenemos la historia actual
        Story currentStory = getStory(storyId);

        // 4. Descartamos reviews pendientes
        characterReviewRepository.changeStatus(characterId, RevisionStatus.DISCARDED);

        // 5. Llamamos al agente
        CharacterReviewAgentResult result = agent.prompt().user(u -> u.text(reviewCharacterResource)
            .param("synopsis", currentStory.getSummary())
            .param("narrative", current.getNarrativeDescription())
            .param("visual", current.getVisualDescription())
            .param("target", request.target().toString())
            .param("hint", request.hint()))
        .call().entity(CharacterReviewAgentResult.class);

        // 6. Persistimos y devolvemos resultado
        return characterReviewRepository.createReview(result, characterId, request);
    }


    @Override
    public CharacterReview getPendingReview(Long storyId, Long characterId) {
        getCharacter(storyId, characterId);
        return characterReviewRepository.getPendingReview(characterId);
    }


    @Override
    public CharacterModel confirmCharacterReview(Long storyId, Long characterId, ConfirmReviewRequestDto request) {
        // 1. Obtenemos personaje actual en bbdd
        CharacterModel current = getCharacter(storyId, characterId);

        // 2. Obtenemos la revision pendiente
        CharacterReview review = characterReviewRepository.getPendingReview(characterId);
        if(review == null)
            throw new AppException("No hay una review pendiente para el character: " + characterId, HttpStatus.CONFLICT);

        if(request.status() == RevisionStatus.DISCARDED)
            characterReviewRepository.changeStatus(characterId, RevisionStatus.DISCARDED);
        else if(request.status() == RevisionStatus.CONFIRMED) {
            characterReviewRepository.changeStatus(characterId, RevisionStatus.CONFIRMED);

            //3. Persistir los camios
            applyChanges(current, review);
            return characterRepository.update(current);
        }


        return current;
    }

    

    private void applyChanges(CharacterModel current, CharacterReview review) {
        current.setNarrativeDescription(review.getNarrativeDescription());
        current.setVisualDescription(review.getVisualDescription());
    }


    private void applyChanges(Story story, StoryReview review) {
        story.setTitle(review.getPreviewStory().getTitle());
        story.setSummary(review.getPreviewStory().getSummary());
        story.setCharacters(review.getPreviewStory().getCharacters());
    }

    @Override
    public byte[] generateCharacter(Long characterId) {
        return null;
    }

    private String readTopic(Topic t) {
        return t + " / " + t.getDescription();
    }

    private String describeCharacters(Story story) {
        return story.getCharacters().stream()
            .map(c -> c.getName() + " ( " + c.getNarrativeDescription() + " / " + c.getVisualDescription() + " )")
            .collect(Collectors.joining("; "));
    }


    @Override
    public List<CharacterModel> getCharacters(Long storyId) {
        return getStory(storyId).getCharacters();
    }


    @Override
    public CharacterModel getCharacter(Long storyId, Long characterId) {
        return getStory(storyId).getCharacters().stream().filter(x -> x.getId().equals(characterId)).findFirst().orElseThrow(() -> {
            throw new ResourceNotFoundException("No se ha encontrado un character con id: " + characterId);
        });
    }
}
