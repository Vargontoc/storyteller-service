package es.vargontoc.storyteller.application.ports.out.persistence;


import es.vargontoc.storyteller.domain.enums.RevisionStatus;
import es.vargontoc.storyteller.domain.model.StoryOutline;
import es.vargontoc.storyteller.domain.model.StoryReview;
import es.vargontoc.storyteller.domain.response.StoryReviewAgentResult;

public interface StoryReviewRepository {
    
    /** Cambia el estado de la revision pendiente. Al confirmar una con escaleta candidata, esta pasa a ser la vigente; al descartar no se toca. */
    void changeStatus(Long storyId, RevisionStatus status);

    StoryReview getPendingReview(Long storyId);

    StoryReview createStoryReview(StoryReviewAgentResult request, Long storyId, String hintRequest);

    /**
     * Guarda una revision con escaleta candidata completa. Calcula los numeros de beat distintos frente a la vigente.
     * Si el hint no se acepta (o no hay candidata) se guarda DISCARDED con su motivo y sin escaleta.
     */
    StoryReview createOutlineReview(Long storyId, String hint, boolean hintAccepted, String rejectedReason, String title, String synopsis, StoryOutline candidate);

    StoryReview getReview(Long idReview);
}
