package es.vargontoc.storyteller.application.ports.in.generator;

import es.vargontoc.storyteller.domain.command.ActorReviewCommand;
import es.vargontoc.storyteller.domain.model.Actor;
import es.vargontoc.storyteller.domain.model.ActorReview;

public interface ActorGeneration extends Reviewable<Actor, ActorReview, ActorReviewCommand>  { }
