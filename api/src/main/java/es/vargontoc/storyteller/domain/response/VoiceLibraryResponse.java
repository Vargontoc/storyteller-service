package es.vargontoc.storyteller.domain.response;

import java.util.List;

import es.vargontoc.storyteller.domain.model.VoiceInfo;

public record VoiceLibraryResponse(List<VoiceInfo> voices, int count) { }
