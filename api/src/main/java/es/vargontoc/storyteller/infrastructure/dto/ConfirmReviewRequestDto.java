package es.vargontoc.storyteller.infrastructure.dto;

import es.vargontoc.storyteller.domain.model.RevisionStatus;
import jakarta.validation.constraints.NotNull;

public record ConfirmReviewRequestDto(
    @NotNull
    RevisionStatus status) {
    
}
