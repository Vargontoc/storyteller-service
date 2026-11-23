package es.vargontoc.storyteller.infrastructure.adapters.in.rest.dto;

import java.util.List;

import es.vargontoc.storyteller.domain.model.VoiceLanguage;

/** Idiomas asignables a una voz y el idioma por defecto (español). */
public record VoiceLanguagesResponse(List<VoiceLanguage> languages, VoiceLanguage defaultLanguage) { }
