package es.vargontoc.storyteller.ports.in;

import java.util.List;

import es.vargontoc.storyteller.domain.Topic;

public interface TopicUseCase {
    
    Topic generate();

    List<Topic> getTopics();
}
