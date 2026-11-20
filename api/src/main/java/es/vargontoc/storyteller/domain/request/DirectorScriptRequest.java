package es.vargontoc.storyteller.domain.request;

/**
 * Entrada para generar un guion: tema, tamaño y ficha del protagonista elegido.
 * {@code protagonistCatalogId} referencia el personaje de catálogo cuyo retrato aprobado ilustra al protagonista.
 */
public record DirectorScriptRequest(
        String topic,
        String description,
        int pages,
        String protagonistName,
        String protagonistNarrative,
        String protagonistVisual,
        Long protagonistCatalogId) {

    public DirectorScriptRequest(String topic, String description, int pages,
            String protagonistName, String protagonistNarrative, String protagonistVisual) {
        this(topic, description, pages, protagonistName, protagonistNarrative, protagonistVisual, null);
    }
}
