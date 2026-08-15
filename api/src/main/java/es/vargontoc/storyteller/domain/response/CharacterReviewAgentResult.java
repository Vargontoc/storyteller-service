package es.vargontoc.storyteller.domain.response;

public record CharacterReviewAgentResult(boolean hintAccepted, String rejectionReason, String narrativeDescription, String visualDescription) { }
