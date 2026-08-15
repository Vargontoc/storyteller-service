package es.vargontoc.storyteller.application.ports.in.generator;

import es.vargontoc.storyteller.domain.command.StoryGenerateCommand;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.model.StoryReview;

public interface StoryGeneration extends Generable<Story, StoryGenerateCommand>, Reviewable<Story, StoryReview, StoryGenerateCommand> { }
