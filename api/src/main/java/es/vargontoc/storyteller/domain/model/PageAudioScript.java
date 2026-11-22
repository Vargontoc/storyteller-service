package es.vargontoc.storyteller.domain.model;

import java.util.List;

/** Guion de audio de una página interior: su número (desde 1) y sus fragmentos en orden. */
public record PageAudioScript(int page, List<AudioFragment> fragments) {
}
