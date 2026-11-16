package es.vargontoc.storyteller.domain.command;
import es.vargontoc.storyteller.domain.enums.RevisionStatus;
public record CatalogCharacterReviewResolutionCommand(Long catalogCharacterId, Long reviewId, RevisionStatus status) { }
