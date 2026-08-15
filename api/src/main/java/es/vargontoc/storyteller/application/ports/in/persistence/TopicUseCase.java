package es.vargontoc.storyteller.application.ports.in.persistence;

import java.util.List;

import es.vargontoc.storyteller.domain.model.Topic;

public interface TopicUseCase {
    
    List<Topic> getTopics();
}
