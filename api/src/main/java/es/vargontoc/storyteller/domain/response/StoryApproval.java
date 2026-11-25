package es.vargontoc.storyteller.domain.response;

import es.vargontoc.storyteller.domain.model.Story;

/** Respuesta de aprobar el guion: el cuento aprobado y si la generación de la portada empezó (no empezar no invalida la aprobación). */
public record StoryApproval(Story story, boolean coverStarted) { }
