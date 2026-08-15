package es.vargontoc.storyteller.application.ports.in.generator;

import es.vargontoc.storyteller.domain.command.StoryPageGenerateCommand;
import es.vargontoc.storyteller.domain.command.StoryPageReviewCommand;
import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.domain.model.StoryPageReview;

public interface StoryPageGeneration extends Generable<StoryPage, StoryPageGenerateCommand>, Reviewable<StoryPage, StoryPageReview,StoryPageReviewCommand> { }
