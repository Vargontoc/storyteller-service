package es.vargontoc.storyteller.infrastructure.persistence;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface CatalogCharacterJpaRepository extends JpaRepository<CatalogCharacterJpaEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select character from CatalogCharacterJpaEntity character where character.id = :id")
    Optional<CatalogCharacterJpaEntity> findByIdForUpdate(Long id);
}
