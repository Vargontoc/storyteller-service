package es.vargontoc.storyteller.ports.out;

import java.util.List;

import es.vargontoc.storyteller.domain.Topic;

/** Interface para la persistencia de Topìcs de historias */
public interface TopicRepository {
    
    /** Crea un nuevo tema */
    Topic createTopic(Topic topic);

    /** Obtiene la lista de temas persistidos */
    List<Topic> getTopics();
}
