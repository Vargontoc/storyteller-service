package es.vargontoc.storyteller.domain.response;

import java.util.List;

public record CharacterAgentResult(boolean mainCharacter, String name, String narrativeDescription, String visualDescription, String visualDescriptionEn, List<String> visualAttributes) { }
