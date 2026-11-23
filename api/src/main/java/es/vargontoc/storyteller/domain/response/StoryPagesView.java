package es.vargontoc.storyteller.domain.response;

import java.util.List;

import es.vargontoc.storyteller.domain.enums.StoryStatus;

/**
 * Portada y páginas de un cuento con escaleta. {@code generating} indica generación en curso del cuento (sin acciones).
 * {@code nextPage} es lo que se puede generar ahora (0 = portada, N = página N) o null si no procede
 * (hay algo pendiente, cuento completo, guion sin aprobar o generación en curso).
 * {@code hasAudio}: el cuento tiene audio publicado; {@code narrating}: se está narrando ahora mismo.
 */
public record StoryPagesView(Long storyId, StoryStatus status, boolean generating, Integer nextPage, boolean hasAudio,
                             boolean narrating, List<PageDetail> pages) { }
