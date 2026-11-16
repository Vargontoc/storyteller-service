package es.vargontoc.storyteller.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortraitCandidateJpaRepository extends JpaRepository<PortraitCandidateJpaEntity, Long> {
    List<PortraitCandidateJpaEntity> findByCatalogCharacterId(Long catalogCharacterId);
    void deleteByCatalogCharacterId(Long catalogCharacterId);
}
