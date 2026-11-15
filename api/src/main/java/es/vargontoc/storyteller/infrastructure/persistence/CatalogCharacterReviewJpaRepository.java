package es.vargontoc.storyteller.infrastructure.persistence;
import es.vargontoc.storyteller.domain.enums.RevisionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CatalogCharacterReviewJpaRepository extends JpaRepository<CatalogCharacterReviewJpaEntity,Long> { boolean existsByCatalogCharacterIdAndStatus(Long catalogCharacterId, RevisionStatus status); java.util.Optional<CatalogCharacterReviewJpaEntity> findByCatalogCharacterIdAndStatus(Long catalogCharacterId, RevisionStatus status); }
