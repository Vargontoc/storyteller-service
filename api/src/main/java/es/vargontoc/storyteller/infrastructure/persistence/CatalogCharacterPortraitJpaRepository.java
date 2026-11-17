package es.vargontoc.storyteller.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogCharacterPortraitJpaRepository extends JpaRepository<CatalogCharacterPortraitJpaEntity, Long> {
    Optional<CatalogCharacterPortraitJpaEntity> findByCatalogCharacterId(Long catalogCharacterId);
    boolean existsByCatalogCharacterId(Long catalogCharacterId);
}
