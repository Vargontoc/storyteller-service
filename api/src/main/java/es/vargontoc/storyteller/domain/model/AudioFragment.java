package es.vargontoc.storyteller.domain.model;

/** Fragmento de una página del guion de audio con la expresividad con la que se sintetiza. */
public record AudioFragment(String text, double exaggeration, double cfgWeight, double temperature) {
}
