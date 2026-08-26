package es.vargontoc.storyteller.infrastructure.adapters.out.persistence;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import es.vargontoc.storyteller.application.ports.out.persistence.StoryPageReviewRepository;
import es.vargontoc.storyteller.domain.enums.PageReviewTarget;
import es.vargontoc.storyteller.domain.enums.RevisionStatus;
import es.vargontoc.storyteller.domain.model.SceneComposition;
import es.vargontoc.storyteller.domain.model.StoryPageReview;
import es.vargontoc.storyteller.domain.response.StoryPageReviewAgentResult;
import es.vargontoc.storyteller.infrastructure.mappers.StoryPageReviewMapper;
import es.vargontoc.storyteller.infrastructure.persistence.StoryPageJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryPageJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.StoryPageReviewJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryPageReviewJpaRepository;

@Repository
public class StoryPageReviewRepositoryAdapter implements StoryPageReviewRepository {

    private final StoryPageReviewJpaRepository repository;
    private final StoryPageJpaRepository pageRepository;
    private final StoryPageReviewMapper mapper;

    public StoryPageReviewRepositoryAdapter(
        StoryPageReviewMapper mapper,
        StoryPageJpaRepository pageRepository,
        StoryPageReviewJpaRepository repository){
        this.mapper = mapper;
        this.repository = repository;
        this.pageRepository = pageRepository;
    }

    @Override
    public StoryPageReview getPendingReview(Long pageId) {
        Optional<StoryPageReviewJpaEntity> anyReview = repository.getActiveStoryReviewByPageId(pageId);
        if(anyReview.isPresent())
            return mapper.toModel(anyReview.get()); 
        return null;
    }

    @Override
    public void changeStatus(Long pageId, RevisionStatus status) {
        repository.getActiveStoryReviewByPageId(pageId).ifPresent(x -> {
            x.setStatus(status);
            x.setUpdatedAt(LocalDateTime.now());
            repository.save(x);
        });
    }

    @Override
    public StoryPageReview createReview(StoryPageReviewAgentResult result, Long pageId, PageReviewTarget target, String hint) {
        StoryPageJpaEntity page = pageRepository.findById(pageId).get();

        String resultText = getRealTextByPage(page, target, result.text());
        SceneComposition resultScene = getSceneByTarget(page, target, result.scene());

        StoryPageReviewJpaEntity entity = StoryPageReviewJpaEntity.candidate(page, hint,result.hintAccepted(), result.rejectedReason(), target ,resultText);
        entity.setComposition(resultScene);
        
        return mapper.toModel(repository.save(entity));
    }

    private SceneComposition getSceneByTarget(StoryPageJpaEntity page, PageReviewTarget target, SceneComposition scene) {
            return switch (target) {
                case TEXT -> page.getSceneComposition();
                case SCENE, BOTH -> scene;
            };
    }

    private String getRealTextByPage(StoryPageJpaEntity page, PageReviewTarget target, String text) {
        if(page.getPage() == 0) {
            return page.getStory().getTitle();
        }else{
            return switch (target) {
                case TEXT, BOTH -> text;
                case SCENE -> page.getText();
            };
        }
    }
    
}
