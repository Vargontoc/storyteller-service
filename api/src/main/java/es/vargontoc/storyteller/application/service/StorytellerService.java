package es.vargontoc.storyteller.application.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import es.vargontoc.storyteller.application.ports.in.persistence.StorytellerUseCase;
import es.vargontoc.storyteller.application.ports.out.ResourceStorage;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryRepository;
import es.vargontoc.storyteller.domain.enums.ResourceType;
import es.vargontoc.storyteller.domain.model.ActorSummary;
import es.vargontoc.storyteller.domain.model.PageSummary;
import es.vargontoc.storyteller.domain.model.PaginatedResponse;
import es.vargontoc.storyteller.domain.model.StorySummary;
import es.vargontoc.storyteller.domain.request.PageAssetRequest;
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
    private final ObjectMapper  mapper = new ObjectMapper();

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
    @Override
    public File downloadStory(Long storyId) {
        // 1. Obtenemos la historia
        StorySummary story = getStory(storyId);
        
        // 2. Comprobar que tiene las mismas paginas generadas (ignorando portada) con su tamaño
        List<PageSummary> pages = getPages(storyId);
        if(story.size() != story.pages())
            throw new AppException("El cuento no se esta generado al completo", HttpStatus.BAD_REQUEST);
        if(pages.stream().anyMatch(p -> assetEmpty(p.image()) || (p.page() != 0 && assetEmpty(p.audio()))))
            throw new AppException("Faltan assets por generar en el cuento", HttpStatus.BAD_REQUEST);

        List<PageAssetRequest> assets = new ArrayList<>();
        pages.forEach(p -> {

            assets.add(new PageAssetRequest(p.page(), ResourceType.IMAGE, p.image()));
            if(p.page() != 0)
                assets.add(new PageAssetRequest(p.page(), ResourceType.AUDIO, p.audio()));
        });

        try {
            String json = mapper.writeValueAsString(Map.of(
                "title", story.title(),
                "pages", readPages(pages)
            ));

            return storage.downloadStory(json, assets);

        }catch(Exception e) {
            throw new AppException("Error creacion de json: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private List<Map<String, Object>> readPages(List<PageSummary> pages) {
        List<Map<String, Object>> result = new ArrayList<>();
        pages.stream().filter(p -> p.page() != 0).forEach(p -> {
            result.add(Map.of(
                "page", p.page(),
                "text", p.text()
            ));
        });
        return result;
    }

    private boolean assetEmpty(String asset) {
        if(asset == null || asset.isBlank()) return true;
        return !storage.existsAsset(asset);
    }
}
