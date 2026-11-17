package es.vargontoc.storyteller.infrastructure.adapters.out.persistence;

import es.vargontoc.storyteller.application.ports.out.external.CharacterImageConditionPort;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterPortraitJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.PortraitCandidateJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class PortraitConditionAdapter implements CharacterImageConditionPort {
    private final CatalogCharacterPortraitJpaRepository approvedPortraits;
    private final PortraitCandidateJpaRepository candidates;

    public PortraitConditionAdapter(CatalogCharacterPortraitJpaRepository approvedPortraits,
            PortraitCandidateJpaRepository candidates) {
        this.approvedPortraits = approvedPortraits;
        this.candidates = candidates;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasApprovedPortrait(Long catalogCharacterId) {
        return catalogCharacterId != null && approvedPortraits.existsByCatalogCharacterId(catalogCharacterId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasGeneratedImages(Long catalogCharacterId) {
        return catalogCharacterId != null && (candidates.existsByCatalogCharacterId(catalogCharacterId)
            || approvedPortraits.existsByCatalogCharacterId(catalogCharacterId));
    }
}
