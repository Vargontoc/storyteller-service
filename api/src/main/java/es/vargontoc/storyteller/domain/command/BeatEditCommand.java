package es.vargontoc.storyteller.domain.command;

import es.vargontoc.storyteller.domain.model.StoryBeat;

/**
 * Edición directa del beat de una página interior (la página 1 es el primer beat).
 * {@code confirmInvalidation}: null = primera petición (si hay páginas, pide confirmación),
 * true = el usuario confirma el aviso, false = lo cancela.
 */
public record BeatEditCommand(Long storyId, int page, StoryBeat beat, Boolean confirmInvalidation) { }
