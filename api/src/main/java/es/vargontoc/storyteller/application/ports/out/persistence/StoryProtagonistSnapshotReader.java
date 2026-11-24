package es.vargontoc.storyteller.application.ports.out.persistence;

import java.util.Optional;

import es.vargontoc.storyteller.domain.model.StoryProtagonistSnapshot;

/** Lectura de la instantánea del protagonista de un cuento; nunca consulta el catálogo vivo. */
public interface StoryProtagonistSnapshotReader {

    /**
     * Nombre, descripción visual y retrato de la instantánea del protagonista; vacío si el cuento no tiene protagonista.
     * Lanza {@code ResourceNotFoundException} si el cuento no existe.
     */
    Optional<StoryProtagonistSnapshot> find(Long storyId);
}
