package es.vargontoc.storyteller.infrastructure.mappers;

import org.springframework.stereotype.Component;

import es.vargontoc.storyteller.domain.Topic;
import es.vargontoc.storyteller.infrastructure.persistence.TopicJpaEntity;
import es.vargontoc.storyteller.shared.mappers.AbstractMapper;

@Component
public class TopicMapper extends AbstractMapper<TopicJpaEntity, Topic> {

    @Override
    public TopicJpaEntity toEntity(Topic model) {
        TopicJpaEntity target = new TopicJpaEntity();
        target.setId(model.getId());
        target.setName(model.getType());
        target.setDescription(model.getDescription());
        
        return target;
    }

    @Override
    public Topic toModel(TopicJpaEntity entity) {
        Topic target = new Topic();
        target.setId(entity.getId());
        target.setType(entity.getName());
        target.setDescription(entity.getDescription());
        return target;
    }
    
}
