package es.vargontoc.storyteller.domain.response;

import es.vargontoc.storyteller.domain.model.SceneComposition;

/** Contenido que devuelve el escritor: texto narrativo y escena (en la portada el texto es {@code null}). */
public record WriterResult(String text, SceneComposition scene) { }
