package es.vargontoc.storyteller.application.ports.in.persistence;

import java.util.Optional;

import es.vargontoc.storyteller.domain.response.PageDetail;
import es.vargontoc.storyteller.domain.response.StoryPagesView;

/** Consulta de la portada y las páginas de un cuento con escaleta. Solo lectura: no hay edición manual. */
public interface PageQuery {

    /** Portada y páginas con sus acciones; vacío si el cuento es antiguo (sin escaleta). */
    Optional<StoryPagesView> findPages(Long storyId);

    /** Una página con sus acciones; vacío si el cuento es antiguo. 404 si la página no es del cuento. */
    Optional<PageDetail> findPage(Long storyId, Long pageId);

    /** Imagen importada del contenido aceptado. 404 si la página no es del cuento o no tiene imagen. */
    byte[] getAcceptedImage(Long storyId, Long pageId);
}
