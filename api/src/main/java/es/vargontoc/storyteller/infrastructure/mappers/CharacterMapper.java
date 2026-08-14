package es.vargontoc.storyteller.infrastructure.mappers;

import org.springframework.stereotype.Component;

import es.vargontoc.storyteller.domain.Character;
import es.vargontoc.storyteller.infrastructure.persistence.CharacterJpaEntity;
import es.vargontoc.storyteller.shared.mappers.AbstractMapper;

@Component
public class CharacterMapper  extends AbstractMapper<CharacterJpaEntity, Character>{

    @Override
    public CharacterJpaEntity toEntity(Character model) {
        CharacterJpaEntity target = new CharacterJpaEntity();
        target.setId(model.getId());
        target.setName(model.getName());
        target.setNarrativeDescription(model.getNarrativeDescription());
        target.setVisualDescription(model.getVisualDescription());
        return target;
    }

    @Override
    public Character toModel(CharacterJpaEntity entity) {
        Character target = new Character();
        target.setId(entity.getId());
        target.setName(entity.getName());
        target.setNarrativeDescription(entity.getNarrativeDescription());
        target.setVisualDescription(entity.getVisualDescription());

        return target;
    }
    
}
