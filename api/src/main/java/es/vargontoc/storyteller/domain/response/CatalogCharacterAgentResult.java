package es.vargontoc.storyteller.domain.response;

import java.util.List;

/** Salida textual del agente al crear una ficha de catálogo. */
public record CatalogCharacterAgentResult(
        boolean accepted,
        String rejectionReason,
        String name,
        String narrativeDescription,
        String visualDescription,
        String visualDescriptionEn,
        List<String> visualAttributes) { }
