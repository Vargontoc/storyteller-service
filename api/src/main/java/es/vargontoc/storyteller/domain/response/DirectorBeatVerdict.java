package es.vargontoc.storyteller.domain.response;

/** Veredicto de edad del director sobre un beat editado directamente. */
public record DirectorBeatVerdict(boolean accepted, String reason) { }
