package es.vargontoc.storyteller.infrastructure.persistence;

import es.vargontoc.framework.persistence.jpa.entity.AutoIdJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "portrait_candidate")
public class PortraitCandidateJpaEntity extends AutoIdJpaEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "catalog_character_id", nullable = false)
    private CatalogCharacterJpaEntity catalogCharacter;

    @ManyToOne(optional = false)
    @JoinColumn(name = "generation_job_id", nullable = false)
    private PortraitGenerationJobJpaEntity generationJob;

    @Column(name = "asset_path", nullable = false, length = 1000)
    private String assetPath;

    public CatalogCharacterJpaEntity getCatalogCharacter() { return catalogCharacter; }
    public void setCatalogCharacter(CatalogCharacterJpaEntity catalogCharacter) { this.catalogCharacter = catalogCharacter; }
    public PortraitGenerationJobJpaEntity getGenerationJob() { return generationJob; }
    public void setGenerationJob(PortraitGenerationJobJpaEntity generationJob) { this.generationJob = generationJob; }
    public String getAssetPath() { return assetPath; }
    public void setAssetPath(String assetPath) { this.assetPath = assetPath; }
}
