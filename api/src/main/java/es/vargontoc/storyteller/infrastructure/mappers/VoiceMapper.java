package es.vargontoc.storyteller.infrastructure.mappers;

import org.springframework.stereotype.Component;

import es.vargontoc.storyteller.domain.model.Voice;
import es.vargontoc.storyteller.infrastructure.persistence.VoiceJpaEntity;
import es.vargontoc.storyteller.shared.mappers.AbstractMapper;

@Component
public class VoiceMapper extends AbstractMapper<VoiceJpaEntity, Voice> {

    @Override
    public VoiceJpaEntity toEntity(Voice model) {
        VoiceJpaEntity target = new VoiceJpaEntity();
        target.setId(model.getId());
        target.setName(model.getName());
        target.setLanguage(model.getLanguage());
        target.setPermissionDeclared(model.isPermissionDeclared());
        return target;
    }

    @Override
    public Voice toModel(VoiceJpaEntity entity) {
        Voice target = new Voice(entity.getName(), entity.getLanguage(), entity.isPermissionDeclared());
        target.setId(entity.getId());
        return target;
    }
}
