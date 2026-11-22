package es.vargontoc.storyteller.infrastructure.mappers;

import org.springframework.stereotype.Component;

import es.vargontoc.storyteller.domain.model.StoryNarration;
import es.vargontoc.storyteller.infrastructure.persistence.StoryNarrationAudioJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.StoryNarrationJpaEntity;
import es.vargontoc.storyteller.shared.mappers.AbstractMapper;

@Component
public class StoryNarrationMapper extends AbstractMapper<StoryNarrationJpaEntity, StoryNarration> {

    /** Solo estado y voz: el cuento y los audios (que llevan fichero) los gestiona el adaptador. */
    @Override
    public StoryNarrationJpaEntity toEntity(StoryNarration model) {
        StoryNarrationJpaEntity target = new StoryNarrationJpaEntity();
        target.setId(model.getId());
        target.setStatus(model.getStatus());
        target.setVoiceName(model.getVoiceName());
        return target;
    }

    @Override
    public StoryNarration toModel(StoryNarrationJpaEntity entity) {
        StoryNarration target = new StoryNarration(
                entity.getStory().getId(),
                entity.getStatus(),
                entity.getVoiceName(),
                entity.getAudios().stream().map(StoryNarrationAudioJpaEntity::getPage).sorted().toList());
        target.setId(entity.getId());
        return target;
    }
}
