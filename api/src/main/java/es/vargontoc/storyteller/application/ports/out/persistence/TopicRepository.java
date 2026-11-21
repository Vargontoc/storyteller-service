package es.vargontoc.storyteller.application.ports.out.persistence;

import java.util.List;

import es.vargontoc.storyteller.domain.model.Topic;

/** Interface para la consulta del catálogo interno de temas */
public interface TopicRepository {

    /** Obtiene la lista de temas del catálogo */
    List<Topic> getTopics();
}
