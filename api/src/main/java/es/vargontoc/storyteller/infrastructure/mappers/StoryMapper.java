package es.vargontoc.storyteller.infrastructure.mappers;

import org.springframework.stereotype.Component;

import es.vargontoc.storyteller.domain.Character;
import es.vargontoc.storyteller.domain.Story;
import es.vargontoc.storyteller.infrastructure.persistence.CharacterJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryJpaEntity;
import es.vargontoc.storyteller.shared.mappers.AbstractMapper;

@Component
public class StoryMapper extends AbstractMapper<StoryJpaEntity, Story> {

    private final AbstractMapper<CharacterJpaEntity, Character> characterMapper;

    public StoryMapper(AbstractMapper<CharacterJpaEntity, Character> characterMapper) {
        this.characterMapper = characterMapper;
    }

    @Override
    public StoryJpaEntity toEntity(Story model) {
        StoryJpaEntity target = new StoryJpaEntity();
        target.setId(model.getId());
        target.setTitle(model.getTitle());
        target.setSynopsis(model.getSummary());
        target.setSize(model.getSize());
        target.setCharacters(characterMapper.toEntity(model.getCharacters()));
        return target;
    }

    @Override
    public Story toModel(StoryJpaEntity entity) {
        Story target = new Story();
        target.setId(entity.getId());
        target.setSize(entity.getSize());
        target.setSummary(entity.getSynopsis());
        target.setTitle(entity.getTitle());

        target.setCharacters(characterMapper.toModel(entity.getCharacters()));
        return target;
    }
    
}
