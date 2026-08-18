package es.vargontoc.storyteller.domain.response;

public record StoryPageReviewAgentResult(String text, String scene, boolean hintAccepted, String rejectedReason) {}
