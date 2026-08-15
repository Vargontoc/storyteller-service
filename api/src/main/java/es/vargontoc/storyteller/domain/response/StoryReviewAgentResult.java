package es.vargontoc.storyteller.domain.response;

import java.util.List;

public record StoryReviewAgentResult(boolean hintAccepted, String rejectionReason, String title, String synopsis, List<CharacterAgentResult> characters) {}
