package es.vargontoc.storyteller.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogCharacterForkJpaRepository extends JpaRepository<CatalogCharacterForkJpaEntity, Long> {
    List<CatalogCharacterForkJpaEntity> findByOriginalCatalogCharacterId(Long catalogCharacterId);
}
