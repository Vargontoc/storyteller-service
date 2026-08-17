package es.vargontoc.storyteller.infrastructure.adapters.out.persistence;

import java.time.LocalDateTime;

import org.springframework.stereotype.Repository;

import es.vargontoc.storyteller.application.ports.out.persistence.StoryPageRepository;
import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.domain.response.StoryPageAgentResult;
import es.vargontoc.storyteller.infrastructure.mappers.StoryPageMapper;
import es.vargontoc.storyteller.infrastructure.persistence.StoryJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.StoryPageJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryPageJpaRepository;
import es.vargontoc.storyteller.shared.validations.AbstractValidator;

@Repository
public class StoryPageRepositoryAdapter implements StoryPageRepository {

    private final AbstractValidator<StoryPageAgentResult> validator;

    private final StoryPageJpaRepository repository;
    private final StoryJpaRepository storyRepository;
    private final StoryPageMapper mapper;

    public StoryPageRepositoryAdapter(
        AbstractValidator<StoryPageAgentResult> validator,
        StoryPageJpaRepository repository,
        StoryJpaRepository storyRepository,
        StoryPageMapper mapper
    ){
        this.validator = validator;
        this.mapper = mapper;
        this.repository = repository;
        this.storyRepository = storyRepository;
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

    
}
