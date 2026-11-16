package es.vargontoc.storyteller.infrastructure.persistence;

import es.vargontoc.framework.persistence.jpa.entity.AutoIdJpaEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "catalog_character_fork")
public class CatalogCharacterForkJpaEntity extends AutoIdJpaEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "original_catalog_character_id", nullable = false)
    private CatalogCharacterJpaEntity originalCatalogCharacter;

    @OneToOne(optional = false)
    @JoinColumn(name = "fork_catalog_character_id", nullable = false, unique = true)
    private CatalogCharacterJpaEntity forkCatalogCharacter;

    public CatalogCharacterJpaEntity getOriginalCatalogCharacter() { return originalCatalogCharacter; }
    public void setOriginalCatalogCharacter(CatalogCharacterJpaEntity originalCatalogCharacter) { this.originalCatalogCharacter = originalCatalogCharacter; }
    public CatalogCharacterJpaEntity getForkCatalogCharacter() { return forkCatalogCharacter; }
    public void setForkCatalogCharacter(CatalogCharacterJpaEntity forkCatalogCharacter) { this.forkCatalogCharacter = forkCatalogCharacter; }
}
