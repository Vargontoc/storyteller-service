package es.vargontoc.storyteller.domain.request;

import java.util.List;

import es.vargontoc.storyteller.domain.enums.CharacterReviewTarget;

/** Contexto inmutable que el agente necesita para revisar una ficha de catálogo. */
public record CatalogCharacterReviewAgentRequest(
        String name,
        String narrativeDescription,
        String visualDescription,
        String visualDescriptionEn,
        List<String> visualAttributes,
        CharacterReviewTarget target,
        String hint) { }
