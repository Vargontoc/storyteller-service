package es.vargontoc.storyteller.domain.command;

import es.vargontoc.storyteller.domain.enums.StorySize;

public record StoryGenerateCommand(Long topicId, StorySize size, Long catalogCharacterId) {
    public StoryGenerateCommand(Long topicId, StorySize size) { this(topicId, size, null); }
}
