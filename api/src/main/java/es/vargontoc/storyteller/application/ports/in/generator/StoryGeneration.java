package es.vargontoc.storyteller.application.ports.in.generator;

import es.vargontoc.storyteller.domain.command.StoryGenerateCommand;
import es.vargontoc.storyteller.domain.command.StoryReviewCommand;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.model.StoryReview;
import es.vargontoc.storyteller.domain.response.StoryApproval;

public interface StoryGeneration extends Generable<Story, StoryGenerateCommand>, Reviewable<Story, StoryReview, StoryReviewCommand> {

    /**
     * Aprobacion explicita del guion: SCRIPT_PENDING pasa a SCRIPT_ACCEPTED y se pide la generacion de la portada.
     * La respuesta indica si la portada empezo; si no puede empezar la aprobacion se completa igualmente, sin reintento.
     */
    StoryApproval approve(Long storyId);
}
