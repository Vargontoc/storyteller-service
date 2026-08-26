package es.vargontoc.storyteller.domain.response;

import es.vargontoc.storyteller.domain.model.SceneComposition;

public record StoryPageReviewAgentResult(String text, SceneComposition scene, boolean hintAccepted, String rejectedReason) {}
