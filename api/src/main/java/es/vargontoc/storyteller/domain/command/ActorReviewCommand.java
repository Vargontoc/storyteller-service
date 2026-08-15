package es.vargontoc.storyteller.domain.command;

import es.vargontoc.storyteller.domain.model.CharacterReviewTarget;

public record ActorReviewCommand(long storyId, long id, String hint, CharacterReviewTarget target) { }
