package es.vargontoc.storyteller.infrastructure.adapters.out.persistence;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Repository;

import es.vargontoc.storyteller.application.ports.out.ResourceStorage;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryPageRepository;
import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.domain.model.StoryPageReview;
import es.vargontoc.storyteller.domain.response.StoryPageAgentResult;
import es.vargontoc.storyteller.infrastructure.mappers.StoryPageMapper;
import es.vargontoc.storyteller.infrastructure.persistence.StoryJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.StoryPageJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryPageJpaRepository;
import es.vargontoc.storyteller.shared.exceptions.ResourceNotFoundException;
import es.vargontoc.storyteller.shared.validations.AbstractValidator;

@Repository
public class StoryPageRepositoryAdapter implements StoryPageRepository {

    private final AbstractValidator<StoryPageAgentResult> validator;

    private final StoryPageJpaRepository repository;
    private final StoryJpaRepository storyRepository;
    private final StoryPageMapper mapper;
    private final ResourceStorage storage;

    public StoryPageRepositoryAdapter(
        AbstractValidator<StoryPageAgentResult> validator,
        StoryPageJpaRepository repository,
        StoryJpaRepository storyRepository,
        StoryPageMapper mapper, ResourceStorage storage
    ){
        this.validator = validator;
        this.mapper = mapper;
        this.repository = repository;
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

        // 4. Devolvemos objeto mapeado
        return mapper.toModel(entity);
    }

    private StoryPageJpaEntity createPage(StoryPageAgentResult result, int page, int maxPages) {
        StoryPageJpaEntity entity = new StoryPageJpaEntity();
        entity.setPage(page);
        entity.setLastPage(page == maxPages);
        entity.setCover(page == 0);
        entity.setScenePrompt(result.promptScene());
        entity.setText(result.text());
        entity.setCreatedAt(LocalDateTime.now());


        return entity;
    }

    @Override
    public StoryPage update(StoryPage page) {


        return null;
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
    public StoryPage updateWithReview(Long idPage, StoryPageReview review) {
        StoryPageJpaEntity entity = repository.findById(idPage).get();
        if(entity.getPage() != 0)
            validator.validate(new StoryPageAgentResult(review.getText(), review.getScene()));

        List<Long> pages = repository.getPagesIdByStory(entity.getStory().getId(), entity.getPage());

        if(entity.getPage() == 0) {
            entity.setScenePrompt(review.getScene());
            entity.setImageAsset(null);
        }else {
            entity.setScenePrompt(review.getScene());
            entity.setText(review.getText());
            entity.setImageAsset(null);
            entity.setAudioAsset(null);
            
            repository.deletePages(entity.getStory().getId(), entity.getPage());
        }
        
        var stored = repository.save(entity);

        storage.deletePageAssets(stored.getStory().getId(), idPage, stored.getPage() == 0 ? List.of() : pages);


        return mapper.toModel(repository.save(entity));

    }

    
}
