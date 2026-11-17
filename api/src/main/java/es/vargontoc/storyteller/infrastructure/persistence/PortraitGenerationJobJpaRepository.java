package es.vargontoc.storyteller.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import es.vargontoc.storyteller.domain.enums.PortraitGenerationStatus;

public interface PortraitGenerationJobJpaRepository extends JpaRepository<PortraitGenerationJobJpaEntity, Long> {
    List<PortraitGenerationJobJpaEntity> findByCatalogCharacterIdAndStatus(Long catalogCharacterId, PortraitGenerationStatus status);
    List<PortraitGenerationJobJpaEntity> findByStatus(PortraitGenerationStatus status);
}
