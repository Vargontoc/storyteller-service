package es.vargontoc.storyteller.application.ports.in.generator;

import es.vargontoc.storyteller.domain.response.AudioStoryAgentResult;

public interface AudioStoryGeneration  {
    
    AudioStoryAgentResult generateAudio(Long storyId);
}
