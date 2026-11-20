package es.vargontoc.storyteller.application.ports.out.persistence;

import java.util.Optional;

import es.vargontoc.storyteller.domain.model.StoryOutline;

/** Persistencia del guion con escaleta. Un cuento sin escaleta es un cuento antiguo. */
public interface StoryOutlineRepository {

    /**
     * Guarda (o reemplaza) la escaleta y el reparto ligero y marca el cuento con la version de guion nueva.
     * Lanza {@link IllegalArgumentException} si la escaleta esta incompleta: no se persiste nada.
     */
    void save(Long storyId, StoryOutline outline);

    /** Escaleta del cuento; vacio si es un cuento antiguo. */
    Optional<StoryOutline> find(Long storyId);

    /** Marcador de version de guion; vacio en cuentos antiguos. */
    Optional<Integer> scriptVersion(Long storyId);
}
