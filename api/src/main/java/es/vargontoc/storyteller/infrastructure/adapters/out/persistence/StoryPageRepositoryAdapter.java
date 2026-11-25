package es.vargontoc.storyteller.infrastructure.adapters.out.persistence;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import es.vargontoc.storyteller.application.ports.out.ResourceStorage;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryPageRepository;
import es.vargontoc.storyteller.domain.enums.PageReviewTarget;
import es.vargontoc.storyteller.domain.model.Actor;
import es.vargontoc.storyteller.domain.enums.PageStatus;
import es.vargontoc.storyteller.domain.model.SceneComposition;
import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.domain.model.StoryPageReview;
import es.vargontoc.storyteller.domain.response.StoryPageAgentResult;
import es.vargontoc.storyteller.infrastructure.mappers.StoryPageMapper;
import es.vargontoc.storyteller.infrastructure.persistence.CharacterJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.StoryPageJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryPageJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.StoryPageReviewJpaRepository;
import es.vargontoc.storyteller.shared.exceptions.AppException;
import es.vargontoc.storyteller.shared.exceptions.ResourceNotFoundException;
import es.vargontoc.storyteller.shared.validations.AbstractValidator;

@Repository
public class StoryPageRepositoryAdapter implements StoryPageRepository {

    private final AbstractValidator<StoryPageAgentResult> validator;

    private final StoryPageJpaRepository repository;
    private final StoryPageReviewJpaRepository pageReviewsRepository;
    private final StoryJpaRepository storyRepository;
    private final StoryPageMapper mapper;
    private final ResourceStorage storage;

    public StoryPageRepositoryAdapter(
        AbstractValidator<StoryPageAgentResult> validator,
        StoryPageJpaRepository repository,
        StoryPageReviewJpaRepository pageReviewsRepository,
        StoryJpaRepository storyRepository,
        StoryPageMapper mapper, ResourceStorage storage
    ){
        this.validator = validator;
        this.mapper = mapper;
        this.repository = repository;
        this.pageReviewsRepository = pageReviewsRepository;
        this.storyRepository = storyRepository;
        this.storage = storage;
    }

    @Override
    public StoryPage create(StoryPageAgentResult result, Long storyId, int page) {
        // 1. Validamos la pagina
        if(page != 0)
            validator.validate(result);

        // 2. Obtenemos story objetivo
        StoryJpaEntity story = storyRepository.findById(storyId).get();
        int maxPages = story.getSize().getPages();

        // 3. Persistimos
        StoryPageJpaEntity entity = createPage(result, page, maxPages);
        entity.setStory(story);
        repository.save(entity);

        storyRepository.setSummary(storyId, result.summary());
        // 4. Devolvemos objeto mapeado
        return mapper.toModel(entity);
    }

    private StoryPageJpaEntity createPage(StoryPageAgentResult result, int page, int maxPages) {
        StoryPageJpaEntity entity = new StoryPageJpaEntity();
        entity.setPage(page);
        entity.setSceneComposition(result.composition());
        entity.setText(result.text());
        entity.setCreatedAt(LocalDateTime.now());


        return entity;
    }

    @Override
    public StoryPage createPending(Long storyId, int page, Long beatId) {
        StoryPageJpaEntity entity = new StoryPageJpaEntity();
        entity.setStory(storyRepository.findById(storyId).orElseThrow());
        entity.setPage(page);
        entity.setStatus(PageStatus.PENDING);
        entity.setBeatId(beatId);
        entity.setCreatedAt(LocalDateTime.now());
        return mapper.toModel(repository.save(entity));
    }

    @Override
    public StoryPage accept(Long idPage, String text, SceneComposition scene) {
        StoryPageJpaEntity entity = repository.findById(idPage).orElseThrow();
        entity.setStatus(PageStatus.ACCEPTED);
        entity.setText(text);
        entity.setSceneComposition(scene);
        entity.setImportedImage(null);
        return mapper.toModel(repository.save(entity));
    }

    @Override
    public StoryPage markAccepted(Long idPage) {
        StoryPageJpaEntity entity = repository.findById(idPage).orElseThrow();
        entity.setStatus(PageStatus.ACCEPTED);
        return mapper.toModel(repository.save(entity));
    }

    @Override
    public StoryPage setImportedImage(Long idPage, String path) {
        StoryPageJpaEntity entity = repository.findById(idPage).orElseThrow();
        entity.setImportedImage(path);
        return mapper.toModel(repository.save(entity));
    }

    @Override
    public StoryPage removeImportedImage(Long idPage) {
        StoryPageJpaEntity entity = repository.findById(idPage).orElseThrow();
        entity.setImportedImage(null);
        return mapper.toModel(repository.save(entity));
    }

    @Override
    public void delete(Long idPage) {
        // story_page_review no cae en cascada: se retiran sus revisiones antes que la página
        pageReviewsRepository.deleteByPageIds(List.of(idPage));
        repository.deleteById(idPage);
    }

    @Override
    public int deleteAfter(Long storyId, int page) {
        List<Long> pageIds = repository.getPagesIdByStory(storyId, page);
        if (pageIds.isEmpty())
            return 0;
        // story_page_version cae en cascada por FK; las revisiones antiguas y los assets se retiran aquí
        pageReviewsRepository.deleteByPageIds(pageIds);
        repository.deletePages(storyId, page);
        storage.deletePageAssets(storyId, pageIds.getFirst(), pageIds.subList(1, pageIds.size()));
        return pageIds.size();
    }

    @Override
    public StoryPage getPage(Long idPage) {
        StoryPageJpaEntity entity = repository.findById(idPage).orElseThrow(() -> {
            throw new ResourceNotFoundException("No se entro pagina con id: " + idPage);
        });


        return mapper.toModel(entity);
    }

    @Override
    public void setImagePath(Long idPage, String path) {
        StoryPageJpaEntity page = repository.findById(idPage).get();
        page.setImageAsset(path);
        repository.save(page);

    }

    @Override
    public void setAudioPath(Long id, String path) {
    
        StoryPageJpaEntity page = repository.findById(id).get();
        page.setAudioAsset(path);
        repository.save(page);

    }

    @Override
    public StoryPage update(Long idPage, StoryPageReview review) {
        StoryPageJpaEntity entity = repository.findById(idPage).get();
        

        List<Long> pages = repository.getPagesIdByStory(entity.getStory().getId(), entity.getPage());

        if(entity.getPage() == 0) {
            entity.setSceneComposition(review.getComposition());
            entity.setImageAsset(null);
        }else {

            if(review.getTarget() == PageReviewTarget.SCENE || review.getTarget() == PageReviewTarget.BOTH)
                entity.setSceneComposition(review.getComposition());

            if(review.getTarget() == PageReviewTarget.TEXT || review.getTarget() == PageReviewTarget.BOTH)
                entity.setText(review.getText());
            
            entity.setImageAsset(null);
            entity.setAudioAsset(null);

            if (!pages.isEmpty())
                pageReviewsRepository.deleteByPageIds(pages);
            repository.deletePages(entity.getStory().getId(), entity.getPage());
        }
        
        // deletePageAssets retira el directorio de la página, imagen importada incluida: se retira también su referencia
        entity.setImportedImage(null);
        var stored = repository.save(entity);

        storage.deletePageAssets(stored.getStory().getId(), idPage, stored.getPage() == 0 ? List.of() : pages);
        storyRepository.setSummary(stored.getStory().getId(), review.getStorySummary());

        return mapper.toModel(repository.save(entity));

    }

    @Override
    public int deleteByStory(Long storyId) {
        List<Long> pageIds = repository.getPagesIdByStory(storyId, -1);
        if (pageIds.isEmpty())
            return 0;
        pageReviewsRepository.deleteByPageIds(pageIds);
        repository.deletePages(storyId, -1);
        // Las páginas eran el origen del resumen acumulado del cuento
        storyRepository.setSummary(storyId, "");
        // Misma política de assets que la revisión de páginas
        storage.deletePageAssets(storyId, pageIds.getFirst(), pageIds.subList(1, pageIds.size()));
        return pageIds.size();
    }

    @Override
    public List<Actor> getSceneActors(Long storyId, List<String> actorNames, boolean isCover) {

        List<CharacterJpaEntity> actors = repository.getCharactersFromPage(storyId, actorNames);
        if(actors.isEmpty()) {
            throw new AppException("No hay ningun personbaje coincidente", HttpStatus.BAD_REQUEST);
        }
        if(isCover) {
            CharacterJpaEntity actor = repository.getMainCharacter(storyId).orElse(actors.getFirst());
            actors = List.of(actor);
        }
        
        return exclusiveMap(actors);
    }

    private List<Actor> exclusiveMap(List<CharacterJpaEntity> actors) {
        List<Actor> result = new ArrayList<>();
        actors.forEach(x -> {
            Actor a = new Actor();
            a.setImage(x.getImagePath());
            a.setName(x.getName());
            a.setMetadata(x.getMetadata());
            result.add(a);
        });
        return result;
    }

    
    
}
