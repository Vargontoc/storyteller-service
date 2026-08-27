package es.vargontoc.storyteller.domain.model;

import es.vargontoc.storyteller.domain.enums.VoiceTonePreset;

public record AudioPage(int page, String text, VoiceTonePreset tone, String word) { }
