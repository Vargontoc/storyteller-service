package es.vargontoc.storyteller.infrastructure.mappers;

import org.springframework.stereotype.Component;

import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.infrastructure.persistence.StoryPageJpaEntity;
import es.vargontoc.storyteller.shared.mappers.AbstractMapper;

@Component
public class StoryPageMapper extends AbstractMapper<StoryPageJpaEntity, StoryPage> {

    @Override
    public StoryPageJpaEntity toEntity(StoryPage model) {
        StoryPageJpaEntity target = new StoryPageJpaEntity();
        target.setId(model.getId());
        target.setText(model.getText());
        target.setSceneComposition(model.getComposition());
        return target;
    }

    @Override
    public StoryPage toModel(StoryPageJpaEntity entity) {
        StoryPage target = new StoryPage();
        target.setId(entity.getId());
        target.setCoverText(entity.getStory().getTitle());
        target.setStoryId(entity.getStory().getId());
        target.setPage(entity.getPage());
        target.setText(entity.getText());
        target.setComposition(entity.getSceneComposition());
        return target;
    }
    
}
