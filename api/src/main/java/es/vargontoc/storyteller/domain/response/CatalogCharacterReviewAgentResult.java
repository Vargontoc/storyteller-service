package es.vargontoc.storyteller.domain.response;

import java.util.List;

/** Salida textual del agente al proponer una revisión de catálogo. */
public record CatalogCharacterReviewAgentResult(
        boolean accepted,
        String rejectionReason,
        String name,
        String narrativeDescription,
        String visualDescription,
        String visualDescriptionEn,
        List<String> visualAttributes) { }
