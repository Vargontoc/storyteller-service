package es.vargontoc.storyteller.domain;

import java.util.List;

public record StoryAgentResult(String title, String synopsis, List<CharacterAgentResult> characters) { }
