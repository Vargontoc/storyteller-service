package es.vargontoc.storyteller.application.ports.out.external;

import es.vargontoc.storyteller.domain.request.CatalogCharacterReviewAgentRequest;
import es.vargontoc.storyteller.domain.response.CatalogCharacterAgentResult;
import es.vargontoc.storyteller.domain.response.CatalogCharacterReviewAgentResult;

/** Puerto para generar y revisar fichas textuales del catálogo de personajes. */
public interface CharacterAgent {

    CatalogCharacterAgentResult generateAutomatically();

    CatalogCharacterAgentResult generateFromHint(String hint);

    CatalogCharacterReviewAgentResult review(CatalogCharacterReviewAgentRequest request);
}
