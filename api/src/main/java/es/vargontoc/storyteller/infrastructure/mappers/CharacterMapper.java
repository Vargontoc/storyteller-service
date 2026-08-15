package es.vargontoc.storyteller.infrastructure.mappers;

import org.springframework.stereotype.Component;

import es.vargontoc.storyteller.domain.CharacterModel;
import es.vargontoc.storyteller.infrastructure.persistence.CharacterJpaEntity;
import es.vargontoc.storyteller.shared.mappers.AbstractMapper;

@Component
public class CharacterMapper  extends AbstractMapper<CharacterJpaEntity, CharacterModel>{

    @Override
    public CharacterJpaEntity toEntity(CharacterModel model) {
        CharacterJpaEntity target = new CharacterJpaEntity();
        target.setId(model.getId());
        target.setName(model.getName());
        target.setNarrativeDescription(model.getNarrativeDescription());
        target.setVisualDescription(model.getVisualDescription());
        return target;
    }

    @Override
    public CharacterModel toModel(CharacterJpaEntity entity) {
        CharacterModel target = new CharacterModel();
        target.setId(entity.getId());
        target.setName(entity.getName());
        target.setNarrativeDescription(entity.getNarrativeDescription());
        target.setVisualDescription(entity.getVisualDescription());

        return target;
    }
    
}
