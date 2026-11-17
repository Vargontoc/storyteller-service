package es.vargontoc.storyteller.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogCharacterForkJpaRepository extends JpaRepository<CatalogCharacterForkJpaEntity, Long> {
    List<CatalogCharacterForkJpaEntity> findByOriginalCatalogCharacterId(Long catalogCharacterId);
    Optional<CatalogCharacterForkJpaEntity> findByForkCatalogCharacterId(Long catalogCharacterId);
}
