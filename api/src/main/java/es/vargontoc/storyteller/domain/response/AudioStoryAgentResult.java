package es.vargontoc.storyteller.domain.response;

import java.util.List;

import es.vargontoc.storyteller.domain.model.AudioPage;

public record AudioStoryAgentResult(List<AudioPage> pages) { }
