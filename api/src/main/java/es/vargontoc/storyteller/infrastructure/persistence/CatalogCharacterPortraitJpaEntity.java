package es.vargontoc.storyteller.infrastructure.persistence;

import es.vargontoc.framework.persistence.jpa.entity.AutoIdJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "catalog_character_portrait")
public class CatalogCharacterPortraitJpaEntity extends AutoIdJpaEntity {
    @OneToOne(optional = false)
    @JoinColumn(name = "catalog_character_id", nullable = false, unique = true)
    private CatalogCharacterJpaEntity catalogCharacter;

    @Column(name = "asset_path", nullable = false, length = 1000)
    private String assetPath;

    public CatalogCharacterJpaEntity getCatalogCharacter() { return catalogCharacter; }
    public void setCatalogCharacter(CatalogCharacterJpaEntity catalogCharacter) { this.catalogCharacter = catalogCharacter; }
    public String getAssetPath() { return assetPath; }
    public void setAssetPath(String assetPath) { this.assetPath = assetPath; }
}
