package es.vargontoc.storyteller.domain.response;

import java.util.List;

public record CharacterReviewAgentResult(boolean hintAccepted, String rejectionReason, String narrativeDescription, String visualDescription, String visualDescriptionEn, List<String> visualAttributes) { }
