package es.vargontoc.storyteller.infrastructure.adapters.out.persistence;

import es.vargontoc.storyteller.infrastructure.adapters.in.rest.web.ApiController;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Repository;

import es.vargontoc.storyteller.application.ports.out.ResourceStorage;
import es.vargontoc.storyteller.application.ports.out.persistence.CharacterRepository;
import es.vargontoc.storyteller.domain.model.Actor;
import es.vargontoc.storyteller.domain.model.ActorMetadata;
import es.vargontoc.storyteller.domain.response.CharacterAgentResult;
import es.vargontoc.storyteller.infrastructure.mappers.CharacterMapper;
import es.vargontoc.storyteller.infrastructure.persistence.CharacterJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.CharacterJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.StoryJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.StoryPageJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.StoryPageReviewJpaRepository;
import es.vargontoc.storyteller.infrastructure.validations.ActorValidation;
import es.vargontoc.storyteller.shared.exceptions.ResourceNotFoundException;

@Repository
public class CharacterRepositoryAdapter implements CharacterRepository {


    private final CharacterJpaRepository repository;
    private final StoryPageJpaRepository pagesRepository;
    private final StoryJpaRepository storyRepository;
    private final StoryPageReviewJpaRepository pageReviewsRepository;
    private final ResourceStorage storage;
    private final CharacterMapper mapper;
    private final ActorValidation validation;

    public CharacterRepositoryAdapter(CharacterJpaRepository repository,
        StoryPageJpaRepository pagesRepository,
        StoryJpaRepository storyRepository,
        StoryPageReviewJpaRepository pageReviewsRepository,
        ResourceStorage storage,
        ActorValidation validation,
        CharacterMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
        this.validation = validation;
        this.pagesRepository = pagesRepository;
        this.pageReviewsRepository = pageReviewsRepository;
        this.storage = storage;
        this.storyRepository = storyRepository;
    }



    @Override
    public Actor update(Actor character) {
        // Validar
        validation.validate(character);

        // Borramos reviews de las paginas afectadas (FK hacia story_page)
        List<Long> pageIds = pagesRepository.getPagesIdByStory(character.getStoryId(), -1);
        if (!pageIds.isEmpty())
            pageReviewsRepository.deleteByPageIds(pageIds);

        // Borramos paginas
        pagesRepository.deletePages(character.getStoryId(), -1);

        CharacterJpaEntity stored = repository.findById(character.getId()).get();
        stored.setNarrativeDescription(character.getNarrativeDescription());
        stored.setVisualDescription(character.getVisualDescription());
        stored.setUpdatedAt(LocalDateTime.now());
        repository.save(stored);
        storyRepository.setSummary(character.getStoryId(), "");
        // Borramos assets
        storage.deleteCharacterAssets(character.getStoryId(), character.getId());
        
        return character;
    }



    @Override
    public Actor getActor(Long id) {
        CharacterJpaEntity entity = repository.findById(id).orElseThrow(() -> {
            throw new ResourceNotFoundException("No se encontro personaje con id: " + id);
        });

        return mapper.toModel(entity);
    }



    @Override
    public List<Actor> getActorsByStory(Long storyId) {
        return null;
    }



    @Override
    public void setImagePath(Long id, String path) {
        CharacterJpaEntity  entity = repository.findById(id).get();
        entity.setImagePath(path);
        repository.save(entity);
    }



    @Override
    public Actor createActor(Long idStory, CharacterAgentResult result) {
        var story =  storyRepository.findById(idStory).get();
        CharacterJpaEntity entity = CharacterJpaEntity.draft(result.mainCharacter(), result.name(), result.visualDescription(), result.narrativeDescription(),
            new ActorMetadata(result.visualDescriptionEn(), result.visualAttributes()));

        entity.setStory(story);
        entity.setCreatedAt(LocalDateTime.now());
        return mapper.toModel(repository.save(entity));
    }
    
}
