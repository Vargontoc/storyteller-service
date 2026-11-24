package es.vargontoc.storyteller.domain.model;

/**
 * Protagonista tal como quedó en el cuento al crearlo (instantánea), no el del catálogo vivo.
 * {@code portraitPath} es la ruta del retrato en el almacén; es {@code null} si la instantánea no tiene retrato.
 */
public record StoryProtagonistSnapshot(String name, String visualDescription, String portraitPath) {
}
