package es.vargontoc.storyteller.application.service;

import es.vargontoc.framework.persistence.jpa.mapping.EntityMapperGuard;
import es.vargontoc.framework.persistence.jpa.transaction.WriteTransactionGuard;
import es.vargontoc.storyteller.application.ports.out.persistence.CatalogCharacterUsagePort;
import es.vargontoc.storyteller.domain.model.ActorMetadata;
import es.vargontoc.storyteller.domain.model.CatalogCharacter;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterForkJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterForkJpaRepository;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterJpaRepository;
import es.vargontoc.storyteller.shared.exceptions.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortraitForkService {
    private final CatalogCharacterJpaRepository catalogCharacters;
    private final CatalogCharacterForkJpaRepository forks;
    private final CatalogCharacterUsagePort usage;

    public PortraitForkService(CatalogCharacterJpaRepository catalogCharacters,
            CatalogCharacterForkJpaRepository forks, CatalogCharacterUsagePort usage) {
        this.catalogCharacters = catalogCharacters;
        this.forks = forks;
        this.usage = usage;
    }

    @Transactional
    public CatalogCharacter create(Long originalCatalogCharacterId, String revisedVisualDescription,
            ActorMetadata revisedMetadata) {
        WriteTransactionGuard.requireWriteTransaction();
        CatalogCharacterJpaEntity original = catalogCharacters.findByIdForUpdate(originalCatalogCharacterId)
            .orElseThrow(() -> new AppException("Catálogo no encontrado", HttpStatus.NOT_FOUND));
        if (!usage.isUsedByStorySnapshot(originalCatalogCharacterId)) {
            throw new AppException("El personaje no está en uso en ningún cuento", HttpStatus.CONFLICT);
        }
        if (blank(revisedVisualDescription) || revisedMetadata == null) {
            throw new AppException("La revisión visual es inválida", HttpStatus.BAD_REQUEST);
        }

        CatalogCharacterJpaEntity fork = new CatalogCharacterJpaEntity();
        fork.setName(original.getName() + " (variante)");
        fork.setNarrativeDescription(original.getNarrativeDescription());
        fork.setVisualDescription(revisedVisualDescription);
        fork.setMetadata(revisedMetadata);
        fork = catalogCharacters.save(fork);

        CatalogCharacterForkJpaEntity relation = new CatalogCharacterForkJpaEntity();
        relation.setOriginalCatalogCharacter(original);
        relation.setForkCatalogCharacter(fork);
        forks.save(relation);
        return map(fork);
    }

    @Transactional
    public CatalogCharacter rename(Long forkCatalogCharacterId, String name) {
        WriteTransactionGuard.requireWriteTransaction();
        CatalogCharacterJpaEntity fork = forks.findByForkCatalogCharacterId(forkCatalogCharacterId)
            .map(CatalogCharacterForkJpaEntity::getForkCatalogCharacter)
            .orElseThrow(() -> new AppException("El personaje no es un fork", HttpStatus.NOT_FOUND));
        if (blank(name)) {
            throw new AppException("El nombre del personaje es inválido", HttpStatus.BAD_REQUEST);
        }
        fork.setName(name);
        return map(catalogCharacters.save(fork));
    }

    private CatalogCharacter map(CatalogCharacterJpaEntity character) {
        return EntityMapperGuard.guard(() -> new CatalogCharacter(character.getId(), character.getName(),
            character.getNarrativeDescription(), character.getVisualDescription(),
            character.getMetadata().translate(), character.getMetadata().attributes()));
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
