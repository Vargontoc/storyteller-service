package es.vargontoc.storyteller.application.ports.out.external;

/**
 * Consulta las condiciones visuales mantenidas por el módulo de assets para un
 * personaje del catálogo. No genera ni modifica imágenes.
 */
public interface CharacterImageConditionPort {

    boolean hasApprovedPortrait(Long catalogCharacterId);

    boolean hasGeneratedImages(Long catalogCharacterId);
}
