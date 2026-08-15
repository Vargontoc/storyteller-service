package es.vargontoc.storyteller.domain.response;

import java.util.List;

public record StoryAgentResult(String title, String synopsis, List<CharacterAgentResult> characters) { }
