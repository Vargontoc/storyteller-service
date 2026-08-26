package es.vargontoc.storyteller.infrastructure.adapters.out.persistence;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Repository;

import es.vargontoc.storyteller.application.ports.out.ResourceStorage;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryRepository;
import es.vargontoc.storyteller.domain.enums.StorySize;
import es.vargontoc.storyteller.domain.model.Actor;
import es.vargontoc.storyteller.domain.model.ActorMetadata;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.response.CharacterAgentResult;
import es.vargontoc.storyteller.domain.response.StoryAgentResult;
import es.vargontoc.storyteller.infrastructure.persistence.CharacterJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.CharacterJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.CharacterReviewJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.StoryJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.StoryPageJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.StoryPageReviewJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.StoryReviewJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.TopicJpaRepository;
import es.vargontoc.storyteller.shared.exceptions.ResourceNotFoundException;
import es.vargontoc.storyteller.shared.mappers.AbstractMapper;
import es.vargontoc.storyteller.shared.validations.AbstractValidator;

@Repository
public class StoryRepositoryAdapter implements StoryRepository {

    private final AbstractValidator<StoryAgentResult> storyValidator;
    private final TopicJpaRepository topicRepository;
    private final CharacterJpaRepository characterJpaRepository;
    private final CharacterReviewJpaRepository characterReviewsRepository;
    private final StoryPageJpaRepository pagesRepository;
    private final StoryPageReviewJpaRepository pageReviewsRepository;
    private final StoryReviewJpaRepository storyReviewsRepository;
    private final StoryJpaRepository repository;
    private final AbstractMapper<StoryJpaEntity, Story> storyMapper;
    private final ResourceStorage storage;

    public StoryRepositoryAdapter(AbstractValidator<StoryAgentResult> storyValidator,
        TopicJpaRepository topicRepository,
        StoryJpaRepository repository,
        AbstractMapper<StoryJpaEntity, Story> mapper,
        StoryPageJpaRepository pagesRepository,
        StoryPageReviewJpaRepository pageReviewsRepository,
        StoryReviewJpaRepository storyReviewsRepository,
        ResourceStorage storage,
        CharacterJpaRepository characterRepository,
        CharacterReviewJpaRepository characterReviewsRepository){
        this.storyValidator = storyValidator;
        this.topicRepository = topicRepository;
        this.repository = repository;
        this.storyMapper = mapper;
        this.characterJpaRepository = characterRepository;
        this.characterReviewsRepository = characterReviewsRepository;
        this.pagesRepository = pagesRepository;
        this.pageReviewsRepository = pageReviewsRepository;
        this.storyReviewsRepository = storyReviewsRepository;
        this.storage = storage;
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
        // 1. Validacion
        storyValidator.validate(new StoryAgentResult(story.getTitle(), story.getSummary(), story.getCharacters().stream().map(this::toMap).toList()));

        // 2. Borramos reviews y luego personajes y paginas (FK hacia character/story_page)
        characterReviewsRepository.deleteByStoryId(story.getId());
        characterJpaRepository.deleteFromStoryId(story.getId());

        List<Long> pageIds = pagesRepository.getPagesIdByStory(story.getId(), -1);
        if (!pageIds.isEmpty())
            pageReviewsRepository.deleteByPageIds(pageIds);
        pagesRepository.deletePages(story.getId(), -1);

        // 1.  Obtenemos el story alamacenado
        StoryJpaEntity stored = repository.findById(story.getId()).get();
        stored.setSynopsis(story.getSummary());
        stored.setTitle(story.getTitle());
        stored.setUpdatedAt(LocalDateTime.now());

        // 3. Guardamos los cambios
        repository.save(stored);

        //4. Borramos assetts
        storage.deleteStoryAssets(story.getId());
        
        // 5. Volvemos a asignar personajes
        story.getCharacters().forEach(c -> createCharacter(stored, c));

        return story;
    }

    private CharacterAgentResult toMap(Actor actor){
        return new CharacterAgentResult(actor.isMain(), actor.getName(), actor.getNarrativeDescription(), actor.getVisualDescription(), actor.getMetadata().translate(), actor.getMetadata().attributes());
    }

    private CharacterJpaEntity createCharacter(StoryJpaEntity stored, CharacterAgentResult c) {
        CharacterJpaEntity entity = CharacterJpaEntity.draft(c.mainCharacter(), c.name(), c.visualDescription(), c.narrativeDescription(),
            new ActorMetadata(c.visualDescriptionEn(), c.visualAttributes()));
        entity.setId(null);
        entity.setStory(stored);
        entity.setCreatedAt(LocalDateTime.now());
        return characterJpaRepository.save(entity);
    }

    private CharacterJpaEntity createCharacter(StoryJpaEntity stored, Actor c) {
        CharacterJpaEntity entity = CharacterJpaEntity.draft(c.isMain(), c.getName(), c.getVisualDescription(), c.getNarrativeDescription(), c.getMetadata());
        entity.setStory(stored);
        entity.setCreatedAt(LocalDateTime.now());
        return characterJpaRepository.save(entity);
    }

    @Override
    public void delete(Long idStory) {
        // Borramos reviews del guion, personajes y paginas antes del story (FK)
        storyReviewsRepository.deleteByStoryId(idStory);
        characterReviewsRepository.deleteByStoryId(idStory);
        characterJpaRepository.deleteFromStoryId(idStory);

        List<Long> pageIds = pagesRepository.getPagesIdByStory(idStory, -1);
        if (!pageIds.isEmpty())
            pageReviewsRepository.deleteByPageIds(pageIds);
        pagesRepository.deletePages(idStory, -1);

        repository.deleteById(idStory);
    }

    @Override
    public Story getStory(Long idStory) {
        StoryJpaEntity entity = repository.findById(idStory).orElseThrow(() -> {
            throw new ResourceNotFoundException("Story not found with id: " + idStory);
        });
        return storyMapper.toModel(entity);
    }


    
}
