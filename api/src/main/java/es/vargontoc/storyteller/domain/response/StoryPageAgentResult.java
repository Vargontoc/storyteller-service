package es.vargontoc.storyteller.domain.response;

import es.vargontoc.storyteller.domain.model.SceneComposition;

public record StoryPageAgentResult(String text, SceneComposition composition, String summary) {}
