package es.vargontoc.storyteller.application.ports.in.persistence;
import es.vargontoc.storyteller.domain.model.Story;

/**
 * 
 */
public interface StoryUseCase {
    
    /** Devuelve una historia según su id */
    Story getStory(Long storyId);

}
