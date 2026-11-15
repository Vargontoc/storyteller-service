package es.vargontoc.storyteller.infrastructure.adapters.out.persistence;
import org.springframework.stereotype.Repository;
import es.vargontoc.storyteller.application.ports.out.persistence.CatalogCharacterPendingReviewReader;
import es.vargontoc.storyteller.domain.enums.RevisionStatus;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterReviewJpaRepository;
@Repository public class CatalogCharacterPendingReviewReaderAdapter implements CatalogCharacterPendingReviewReader {
 private final CatalogCharacterReviewJpaRepository repository;
 public CatalogCharacterPendingReviewReaderAdapter(CatalogCharacterReviewJpaRepository repository){this.repository=repository;}
 public boolean hasPendingReview(Long id){return repository.existsByCatalogCharacterIdAndStatus(id, RevisionStatus.PENDING);}
}
