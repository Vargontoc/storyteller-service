package es.vargontoc.storyteller.infrastructure.mappers;

import org.springframework.stereotype.Component;

import es.vargontoc.storyteller.domain.model.StoryPageReview;
import es.vargontoc.storyteller.infrastructure.persistence.StoryPageReviewJpaEntity;
import es.vargontoc.storyteller.shared.mappers.AbstractReviewMapper;

@Component
public class StoryPageReviewMapper extends AbstractReviewMapper<StoryPageReviewJpaEntity, StoryPageReview> {

    @Override
    public StoryPageReviewJpaEntity toEntity(StoryPageReview model) {
        StoryPageReviewJpaEntity target = new StoryPageReviewJpaEntity();
        
        target.setId(model.getId());
        target.setHint(model.getHint());
        target.setHintAccepted(model.isHintAccepted());
        target.setRejectedReason(model.getRejectedReason());
        target.setStatus(model.getStatus());

        target.setText(model.getText());
        target.setScene(model.getScene());
        
        return target;
    }

    @Override
    public StoryPageReview toModel(StoryPageReviewJpaEntity entity) {
        StoryPageReview target = new StoryPageReview();

        target.setId(entity.getId());
        target.setHint(entity.getHint());
        target.setHintAccepted(entity.isHintAccepted());
        target.setRejectedReason(entity.getRejectedReason());
        target.setStatus(entity.getStatus());
        
        target.setScene(entity.getScene());
        target.setStoryId(entity.getPage().getStory().getId());
        target.setIdPage(entity.getPage().getId());
        target.setPage(entity.getPage().getPage());
        
        return target;
    }
}
