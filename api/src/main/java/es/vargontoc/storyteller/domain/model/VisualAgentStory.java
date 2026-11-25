package es.vargontoc.storyteller.domain.model;

import java.util.List;

/**
 * Cuento listo para exportar al agente visual: título, protagonista de la instantánea,
 * escena de la portada y escenas de las páginas interiores en orden.
 */
public record VisualAgentStory(String title, StoryProtagonistSnapshot protagonist, Cover cover, List<Page> pages) {

    /** Escena de la portada: su descripción y su propio lugar y tono (el texto es el título). */
    public record Cover(String description, String setting, String tone) {
    }

    /** Escena de una página interior: texto aceptado, descripción, lugar del beat y tono del beat. */
    public record Page(String text, String description, String setting, String tone) {
    }
}
