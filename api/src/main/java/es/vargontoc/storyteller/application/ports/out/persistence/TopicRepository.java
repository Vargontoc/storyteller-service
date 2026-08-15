package es.vargontoc.storyteller.application.ports.out.persistence;

import java.util.List;

import es.vargontoc.storyteller.domain.model.Topic;

/** Interface para la persistencia de Topìcs de historias */
public interface TopicRepository {
    
    /** Crea un nuevo tema */
    Topic createTopic(Topic topic);

    /** Obtiene la lista de temas persistidos */
    List<Topic> getTopics();

    /** Obtiene el topic relacionado a un story */
    Topic getTopicByStoryId(Long storyId);
}
