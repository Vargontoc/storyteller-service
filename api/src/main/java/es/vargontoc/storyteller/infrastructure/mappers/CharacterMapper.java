package es.vargontoc.storyteller.infrastructure.mappers;

import org.springframework.stereotype.Component;

import es.vargontoc.storyteller.domain.model.Actor;
import es.vargontoc.storyteller.infrastructure.persistence.CharacterJpaEntity;
import es.vargontoc.storyteller.shared.mappers.AbstractMapper;

@Component
public class CharacterMapper  extends AbstractMapper<CharacterJpaEntity, Actor>{

    @Override
    public CharacterJpaEntity toEntity(Actor model) {
        CharacterJpaEntity target = new CharacterJpaEntity();
        target.setId(model.getId());
        target.setName(model.getName());
        target.setNarrativeDescription(model.getNarrativeDescription());
        target.setVisualDescription(model.getVisualDescription());

        return target;
    }

    @Override
    public Actor toModel(CharacterJpaEntity entity) {
        Actor target = new Actor();
        target.setId(entity.getId());
        target.setStoryId(entity.getStory().getId());
        target.setName(entity.getName());
        target.setNarrativeDescription(entity.getNarrativeDescription());
        target.setVisualDescription(entity.getVisualDescription());
        target.setMetadata(entity.getMetadata());
        return target;
    }
    
}
