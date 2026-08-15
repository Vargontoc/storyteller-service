package es.vargontoc.storyteller.domain;

public record CharacterReviewAgentResult(boolean hintAccepted, String rejectionReason, String narrativeDescription, String visualDescription) { }
