package es.vargontoc.storyteller.ports.in;

import es.vargontoc.storyteller.domain.Story;
import es.vargontoc.storyteller.domain.StoryReview;
import es.vargontoc.storyteller.infrastructure.dto.StoryRequestDto;

/**
 * Interface de casos de uso durante la creación inicial de una historia
 */
public interface StoryUseCase {
    
    /** Devuelve una historia generada por el agente responsable */
    Story generateStory(StoryRequestDto request);

    /** Devuelve una historia según su id */
    Story getStory(Long storyId);

    /** Devuelve una historia nueva en base a un cambio realizado por el usuario */
    StoryReview reviewStory(Long storyId, String hint);

    /** Devuelve un review de una historia pendiente */
    StoryReview getPendingReview(Long storyId);

    /** Devuelve la historia resultante tras la confirmacion del usuario */
    Story confirmReviewStory(Long storyId, boolean confirm);

    /** Devuelve un un personaje nuevo en base a un cambio propuesto por el usuario */
    Character reviewCharacter(Long characterId, String hint);

    /** Devuelve el personaje resultante tras la confirmación del usuario */
    Character confirmCharacterReview(Long reviewId);

    /** Devuelve una imagen en bytes */
    byte[] generateCharacter(Long characterId);

}
