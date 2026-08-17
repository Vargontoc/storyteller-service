package es.vargontoc.storyteller.domain.command;

import es.vargontoc.storyteller.domain.enums.PageReviewTarget;

public record StoryPageReviewCommand(Long pageId, String hint, PageReviewTarget target) { }
