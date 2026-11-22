package es.vargontoc.storyteller.application.ports.out.persistence;

import java.util.List;

import es.vargontoc.storyteller.domain.model.Actor;
import es.vargontoc.storyteller.domain.model.SceneComposition;
import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.domain.model.StoryPageReview;
import es.vargontoc.storyteller.domain.response.StoryPageAgentResult;

public interface StoryPageRepository {
    
    StoryPage create(StoryPageAgentResult result, Long storyId, int page);

    /** Cuentos con escaleta: crea la portada (page 0, sin beat) o la página N pendiente, sin contenido aceptado ni imagen. */
    StoryPage createPending(Long storyId, int page, Long beatId);

    /** Cuentos con escaleta: deja la página aceptada con ese contenido (texto null en la portada) y sin imagen importada. */
    StoryPage accept(Long idPage, String text, SceneComposition scene);

    /** Quita solo la asociación con la imagen importada; el contenido aceptado no cambia. */
    StoryPage removeImportedImage(Long idPage);

    /** Elimina una sola página recién creada (sin revisiones ni versiones). */
    void delete(Long idPage);

    /** Elimina las páginas del cuento con número mayor que {@code page} (con revisiones, versiones y assets) y devuelve cuántas. */
    int deleteAfter(Long storyId, int page);

    StoryPage getPage(Long idPage);

    StoryPage update(Long idPage, StoryPageReview review);

    void setImagePath(Long idPage, String path);

    void setAudioPath(Long id, String path);

    /** Elimina todas las páginas del cuento (con sus revisiones y assets) y devuelve cuántas se retiraron. */
    int deleteByStory(Long storyId);

    List<Actor> getSceneActors(Long storyId, List<String> actorNames, boolean isCover);
    
}
