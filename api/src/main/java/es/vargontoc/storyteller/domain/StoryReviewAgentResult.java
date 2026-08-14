package es.vargontoc.storyteller.domain;

import java.util.List;

public record StoryReviewAgentResult(boolean hintAccepted, String rejectionReason, String title, String synopsis, List<CharacterAgentResult> characters) {}
