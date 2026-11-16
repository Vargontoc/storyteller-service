package es.vargontoc.storyteller.infrastructure.persistence;
import es.vargontoc.storyteller.domain.enums.RevisionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
public interface CatalogCharacterReviewJpaRepository extends JpaRepository<CatalogCharacterReviewJpaEntity,Long> { boolean existsByCatalogCharacterIdAndStatus(Long catalogCharacterId, RevisionStatus status); java.util.Optional<CatalogCharacterReviewJpaEntity> findByCatalogCharacterIdAndStatus(Long catalogCharacterId, RevisionStatus status); @Lock(LockModeType.PESSIMISTIC_WRITE) java.util.Optional<CatalogCharacterReviewJpaEntity> findByIdAndStatus(Long id, RevisionStatus status); }
