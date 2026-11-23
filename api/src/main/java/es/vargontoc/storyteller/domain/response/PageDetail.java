package es.vargontoc.storyteller.domain.response;

import java.util.List;

import es.vargontoc.storyteller.domain.enums.PageAction;
import es.vargontoc.storyteller.domain.enums.PageStatus;

/**
 * Portada (page 0) o página de un cuento con escaleta.
 * {@code title} solo en la portada. {@code accepted} es null mientras está pendiente. {@code versions} es el histórico de una
 * página pendiente y {@code proposals} el de una aceptada reabierta; la otra lista va vacía.
 * {@code actions} lo calcula el servidor según estado, avisos y generación en curso.
 * {@code hasAudio}: la página interior tiene audio publicado; la portada nunca.
 */
public record PageDetail(Long id, int page, PageStatus status, Long beatId, String title, AcceptedContent accepted,
                         List<PageVersionView> versions, List<PageVersionView> proposals, List<PageAction> actions,
                         boolean hasAudio) { }
