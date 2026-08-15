package es.vargontoc.storyteller.ports.in;

import java.util.List;

import es.vargontoc.storyteller.domain.CharacterModel;
import es.vargontoc.storyteller.domain.CharacterReview;
import es.vargontoc.storyteller.domain.Story;
import es.vargontoc.storyteller.domain.StoryReview;
import es.vargontoc.storyteller.infrastructure.dto.ConfirmReviewRequestDto;
import es.vargontoc.storyteller.infrastructure.dto.ReviewCharacterRequestDto;
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
    Story confirmReviewStory(Long storyId, ConfirmReviewRequestDto request);

    List<CharacterModel> getCharacters(Long storyId);

    /** Obtiene un personaje por su id y su story id */
    CharacterModel getCharacter(Long storyId, Long characterId);

    /** Devuelve un un personaje nuevo en base a un cambio propuesto por el usuario */
    CharacterReview reviewCharacter(Long storyId, Long characterId, ReviewCharacterRequestDto request);

    /** Devuelve un CharacterReview pendiente de aceptación o descarte */
    CharacterReview getPendingReview(Long storyId, Long characterId);

    /** Devuelve el personaje resultante tras la confirmación del usuario o descarte */
    CharacterModel confirmCharacterReview(Long storyId, Long characterId, ConfirmReviewRequestDto request);

    /** Devuelve una imagen en bytes */
    byte[] generateCharacter(Long story, Long characterId);

}
