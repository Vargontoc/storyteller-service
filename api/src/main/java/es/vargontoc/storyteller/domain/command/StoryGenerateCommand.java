package es.vargontoc.storyteller.domain.command;

import es.vargontoc.storyteller.domain.enums.StorySize;

public record StoryGenerateCommand(String topic, String description, StorySize size, Long catalogCharacterId) {
    public StoryGenerateCommand(String topic, String description, StorySize size) { this(topic, description, size, null); }
}
