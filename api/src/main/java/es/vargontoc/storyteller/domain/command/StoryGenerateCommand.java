package es.vargontoc.storyteller.domain.command;

import es.vargontoc.storyteller.domain.model.StorySize;

public record StoryGenerateCommand(Long topicId, StorySize size) { }
