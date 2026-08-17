package es.vargontoc.storyteller.infrastructure.adapters.out.persistence;

import java.time.LocalDateTime;

import org.springframework.stereotype.Repository;

import es.vargontoc.storyteller.application.ports.out.persistence.StoryRepository;
import es.vargontoc.storyteller.domain.enums.StorySize;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.response.CharacterAgentResult;
import es.vargontoc.storyteller.domain.response.StoryAgentResult;
import es.vargontoc.storyteller.infrastructure.persistence.CharacterJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.CharacterJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.StoryJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.TopicJpaRepository;
import es.vargontoc.storyteller.shared.exceptions.ResourceNotFoundException;
import es.vargontoc.storyteller.shared.mappers.AbstractMapper;
import es.vargontoc.storyteller.shared.validations.AbstractValidator;

@Repository
public class StoryRepositoryAdapter implements StoryRepository {

    private final AbstractValidator<StoryAgentResult> storyValidator;
    private final TopicJpaRepository topicRepository;
    private final CharacterJpaRepository characterJpaRepository;
    private final StoryJpaRepository repository;
    private final AbstractMapper<StoryJpaEntity, Story> storyMapper;

    public StoryRepositoryAdapter(AbstractValidator<StoryAgentResult> storyValidator, 
        TopicJpaRepository topicRepository,
        StoryJpaRepository repository,
        AbstractMapper<StoryJpaEntity, Story> mapper,
        CharacterJpaRepository characterRepository){
        this.storyValidator = storyValidator;
        this.topicRepository = topicRepository;
        this.repository = repository;
        this.storyMapper = mapper;
        this.characterJpaRepository = characterRepository;
    }

    @Override
    public Story create(StoryAgentResult result, StorySize size, Long topic) {
        // 1. Validamos resultado
        storyValidator.validate(result);

        // 2. Transformamos a story
        StoryJpaEntity entity = StoryJpaEntity.draft(topicRepository.findById(topic).get(), size, result.title(), result.synopsis());
        entity.setCreatedAt(LocalDateTime.now());
        StoryJpaEntity stored = repository.save(entity);
        
        // 3. Creamos los personajes
        result.characters().forEach(c -> stored.getCharacters().add(createCharacter(stored, c)));
        
        // 4. Devolvemos el resultado mapeado
        return storyMapper.toModel(stored);
    }

    @Override
    public Story update(Story story) {

        // 2. Borramos personajes
        characterJpaRepository.deleteFromStoryId(story.getId());

        // 1.  Obtenemos el story alamacenado
        StoryJpaEntity stored = repository.findById(story.getId()).get();
        stored.setSynopsis(story.getSummary());
        stored.setTitle(story.getTitle());
        stored.setUpdatedAt(LocalDateTime.now());

        // 3. Guardamos los cambios
        repository.save(stored);

        // 4. Volvemos a asignar personajes
        story.getCharacters().forEach(c -> createCharacter(stored, c));

        return story;
    }

    private CharacterJpaEntity createCharacter(StoryJpaEntity stored, CharacterAgentResult c) {
        CharacterJpaEntity entity = CharacterJpaEntity.draft(c.name(), c.visualDescription(), c.narrativeDescription());
        entity.setId(null);
        entity.setStory(stored);
        entity.setCreatedAt(LocalDateTime.now());
        return characterJpaRepository.save(entity);
    }

    private CharacterJpaEntity createCharacter(StoryJpaEntity stored, es.vargontoc.storyteller.domain.model.Actor c) {
        CharacterJpaEntity entity = CharacterJpaEntity.draft(c.getName(), c.getVisualDescription(), c.getNarrativeDescription());
        entity.setStory(stored);
        entity.setCreatedAt(LocalDateTime.now());
        return characterJpaRepository.save(entity);
    }

    @Override
    public Story getStory(Long idStory) {
        StoryJpaEntity entity = repository.findById(idStory).orElseThrow(() -> {
            throw new ResourceNotFoundException("Story not found with id: " + idStory);
        });
        return storyMapper.toModel(entity);
    }


    
}
