package es.vargontoc.storyteller.domain.model;

import java.util.List;

public record CatalogCharacter(Long id, String name, String narrativeDescription,
        String visualDescription, String visualDescriptionEn, List<String> visualAttributes) { }
