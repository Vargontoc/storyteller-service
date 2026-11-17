package es.vargontoc.storyteller.domain.command;
import es.vargontoc.storyteller.domain.enums.RevisionStatus;
public record CatalogCharacterReviewResolutionCommand(Long catalogCharacterId, Long reviewId, RevisionStatus status,
        boolean confirmFork) {
    public CatalogCharacterReviewResolutionCommand(Long catalogCharacterId, Long reviewId, RevisionStatus status) {
        this(catalogCharacterId, reviewId, status, false);
    }
}
