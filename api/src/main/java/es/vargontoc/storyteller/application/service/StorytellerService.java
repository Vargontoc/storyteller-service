package es.vargontoc.storyteller.application.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import es.vargontoc.storyteller.application.ports.in.persistence.StorytellerUseCase;
import es.vargontoc.storyteller.application.ports.out.ResourceStorage;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryRepository;
import es.vargontoc.storyteller.domain.model.ActorSummary;
import es.vargontoc.storyteller.domain.model.PageSummary;
import es.vargontoc.storyteller.domain.model.PaginatedResponse;
import es.vargontoc.storyteller.domain.model.StorySummary;
import es.vargontoc.storyteller.infrastructure.persistence.CharacterJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.StoryPageJpaEntity;
import es.vargontoc.storyteller.shared.exceptions.AppException;
import es.vargontoc.storyteller.shared.exceptions.ResourceNotFoundException;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class StorytellerService implements StorytellerUseCase {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final StoryJpaRepository repository;
    private final ResourceStorage storage;
    private final StoryRepository storyRepository;
    public StorytellerService(StoryJpaRepository repository, ResourceStorage storage, StoryRepository storyRepository){
        this.repository = repository;
        this.storage = storage;
        this.storyRepository = storyRepository;
    }
    @Override
    public PaginatedResponse<StorySummary> getStories(PageRequest request) {
        PageRequest pageRequest = request != null ? request : PageRequest.of(0, DEFAULT_PAGE_SIZE);
        return PaginatedResponse.from(repository.findAll(pageRequest).map(this::toSummary));
    }

    private StorySummary toSummary(StoryJpaEntity entity) {
        @SuppressWarnings("null")
        String cover = entity.getPages().stream()
            .filter(x -> x.getPage() == 0)
            .findFirst()
            .map(StoryPageJpaEntity::getImageAsset)
            .orElse(null);

        long generatedPages = entity.getPages().stream()
            .filter(p -> p.getPage() != 0)
            .count();

        return new StorySummary(
            entity.getId(),
            cover,
            entity.getTitle(),
            entity.getSynopsis(),
            entity.getCharacters().size(),
            entity.getSize().getPages(),
            (int) generatedPages
        );
    }

    @Override
    public void deleteStory(Long id) {
        storyRepository.delete(id);
        storage.deleteStoryAssets(id);
    }
    @Override
    public StorySummary getStory(Long id) {
        return repository.findById(id).map(this::toSummary).orElseThrow(() -> {
            throw new ResourceNotFoundException("Story not found with id:"  + id);
        });
    }
    @Override
    public List<ActorSummary> getActors(Long storyId) {
        StoryJpaEntity story = repository.findById(storyId).orElseThrow(() -> {
            throw new ResourceNotFoundException("Story not found with id: " + storyId);
        });

        return story.getCharacters().stream().map(this::toActorSummary).toList();
    }

    @Override
    public ActorSummary getActor(Long storyId, Long actorId) {
        StoryJpaEntity story = repository.findById(storyId).orElseThrow(() -> {
            throw new ResourceNotFoundException("Story not found with id: " + storyId);
        });

        return story.getCharacters().stream().filter(x -> x.getId() == actorId).findFirst().map(this::toActorSummary).orElseThrow(() -> {
            throw new ResourceNotFoundException("Character not found with id: " + actorId + " for story:" + storyId);
        });
    }

    private ActorSummary toActorSummary(CharacterJpaEntity entity) {
        return new ActorSummary(
            entity.getId(),
            entity.getName(),
            entity.getNarrativeDescription(),
            entity.getVisualDescription(),
            entity.getImagePath());
    }
    @Override
    public List<PageSummary> getPages(Long storyId) {
        StoryJpaEntity story = repository.findById(storyId).orElseThrow(() -> {
            throw new ResourceNotFoundException("Story not found with id: " + storyId);
        });
        return story.getPages().stream().map(this::toPageSummary).toList();
    }
    @Override
    public PageSummary getPage(Long storyId, Long pageId) {
        StoryJpaEntity story = repository.findById(storyId).orElseThrow(() -> {
            throw new ResourceNotFoundException("Story not found with id: " + storyId);
        });

        return story.getPages().stream().filter(x -> x.getId() == pageId).findFirst().map(this::toPageSummary).orElseThrow(() -> {
            throw new ResourceNotFoundException("Page not found with id: " + pageId + " for story:" + storyId);
        });
    }

    PageSummary toPageSummary(StoryPageJpaEntity entity) {
        return new PageSummary(
            entity.getId(),
            entity.getPage(),
            entity.getPage() == 0 ? entity.getStory().getTitle() : entity.getText(),
            entity.getSceneComposition().scene(),
            entity.getImageAsset(),
            entity.getAudioAsset());
    }

    @Override
    public byte[] getResource(String path) {
        if(path == null || path.isBlank())
            throw new AppException("Resource empty path", HttpStatus.BAD_REQUEST);
        byte[] resource = storage.getResource(path);

        if(resource == null)
            throw new ResourceNotFoundException("Resource not found on path: " + path);
        return resource;
    }
}
