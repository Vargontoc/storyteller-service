package es.vargontoc.storyteller.infrastructure.persistence;

import es.vargontoc.framework.persistence.jpa.entity.AutoIdJpaEntity;
import es.vargontoc.storyteller.domain.enums.PortraitGenerationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "portrait_generation_job")
public class PortraitGenerationJobJpaEntity extends AutoIdJpaEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "catalog_character_id", nullable = false)
    private CatalogCharacterJpaEntity catalogCharacter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PortraitGenerationStatus status;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    public CatalogCharacterJpaEntity getCatalogCharacter() { return catalogCharacter; }
    public void setCatalogCharacter(CatalogCharacterJpaEntity catalogCharacter) { this.catalogCharacter = catalogCharacter; }
    public PortraitGenerationStatus getStatus() { return status; }
    public void setStatus(PortraitGenerationStatus status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
