package es.vargontoc.storyteller.infrastructure.adapters.in.rest.web;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.vargontoc.storyteller.application.ports.in.generator.StoryGeneration;
import es.vargontoc.storyteller.application.ports.in.generator.TopicGenerator;
import es.vargontoc.storyteller.domain.command.StoryGenerateCommand;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.model.Topic;
import es.vargontoc.storyteller.shared.ApiResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/v1/generations")
public class GenerationsController
{
    private final TopicGenerator topicGenerator;
    private final StoryGeneration storyGenerator;

    public GenerationsController(TopicGenerator topicGenerator, StoryGeneration storyGenerator) {
        this.topicGenerator = topicGenerator;
        this.storyGenerator = storyGenerator;
    }

    @PostMapping("/topic")
    public ResponseEntity<ApiResponse<Topic>> generateTopic() {
        return ResponseEntity.ok(ApiResponse.ok(topicGenerator.generate(null)));
    }

    @PostMapping("/story")
    public ResponseEntity<ApiResponse<Story>>  generateStory(@RequestBody StoryGenerateCommand cmd) {
        return ResponseEntity.ok(ApiResponse.ok(storyGenerator.generate(cmd)));
    }
}
