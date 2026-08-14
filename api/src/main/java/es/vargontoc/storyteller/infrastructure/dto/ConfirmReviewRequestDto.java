package es.vargontoc.storyteller.infrastructure.dto;

import es.vargontoc.storyteller.domain.RevisionStatus;
import jakarta.validation.constraints.NotNull;

public record ConfirmReviewRequestDto(
    @NotNull
    RevisionStatus status) {
    
}
