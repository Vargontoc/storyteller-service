package es.vargontoc.storyteller.infrastructure.adapters.out.persistence;

import es.vargontoc.storyteller.application.ports.out.persistence.CatalogCharacterUsagePort;
import es.vargontoc.storyteller.infrastructure.persistence.CharacterJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class CatalogCharacterUsageAdapter implements CatalogCharacterUsagePort {
    private final CharacterJpaRepository characters;

    public CatalogCharacterUsageAdapter(CharacterJpaRepository characters) {
        this.characters = characters;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUsedByStorySnapshot(Long catalogCharacterId) {
        return catalogCharacterId != null && characters.existsByCatalogCharacterId(catalogCharacterId);
    }
}
