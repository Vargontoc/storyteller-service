package es.vargontoc.storyteller.application.ports.in.generator;

import es.vargontoc.storyteller.domain.command.TopicGenerateCommand;
import es.vargontoc.storyteller.domain.model.Topic;

public interface TopicGenerator extends Generable<Topic, TopicGenerateCommand> {
    
}
