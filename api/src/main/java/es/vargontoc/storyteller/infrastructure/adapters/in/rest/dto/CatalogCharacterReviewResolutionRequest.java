package es.vargontoc.storyteller.infrastructure.adapters.in.rest.dto;
import es.vargontoc.storyteller.domain.enums.RevisionStatus;
import jakarta.validation.constraints.NotNull;
public record CatalogCharacterReviewResolutionRequest(@NotNull RevisionStatus status, boolean confirmFork) { 
    public CatalogCharacterReviewResolutionRequest(RevisionStatus status) { this(status, false); }
}
