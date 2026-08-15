package es.vargontoc.storyteller.infrastructure.adapters.in.rest.dto;

import es.vargontoc.storyteller.domain.model.RevisionStatus;
import jakarta.validation.constraints.NotNull;

public record ConfirmReviewRequestDto(
    Long entityId,
    @NotNull
    RevisionStatus status) {
    
}
