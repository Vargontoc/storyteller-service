package es.vargontoc.storyteller.infrastructure.adapters.in.rest.web;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.vargontoc.storyteller.application.ports.in.generator.AudioStoryGeneration;
import es.vargontoc.storyteller.application.ports.in.generator.StoryGeneration;
import es.vargontoc.storyteller.application.ports.in.generator.StoryPageGeneration;
import es.vargontoc.storyteller.application.ports.in.generator.TopicGenerator;
import es.vargontoc.storyteller.domain.command.StoryGenerateCommand;
import es.vargontoc.storyteller.domain.command.StoryPageGenerateCommand;
import es.vargontoc.storyteller.domain.command.TopicGenerateCommand;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.domain.model.Topic;
import es.vargontoc.storyteller.domain.response.AudioStoryAgentResult;
import es.vargontoc.storyteller.shared.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;



@RestController
@RequestMapping("/api/v1/generations")
@Tag(name = "Generators")
public class GenerationsController
{
    private final TopicGenerator topicGenerator;
    private final StoryGeneration storyGenerator;
    private final StoryPageGeneration pageGeneration;
    private final AudioStoryGeneration audioStoryGeneration;

    public GenerationsController(TopicGenerator topicGenerator,
        StoryGeneration storyGenerator, 
        StoryPageGeneration pageGeneration, AudioStoryGeneration audioStoryGeneration) {
        this.topicGenerator = topicGenerator;
        this.storyGenerator = storyGenerator;
        this.pageGeneration = pageGeneration;
        this.audioStoryGeneration = audioStoryGeneration;
    }

    @PostMapping("/topic")
    @Operation(description = "El Agente genera un nuevo tema para cuentos infantiles")
    public ResponseEntity<ApiResponse<Topic>> generateTopic() {
        return ResponseEntity.ok(ApiResponse.ok(topicGenerator.generate(new TopicGenerateCommand())));
    }

    @PostMapping("/story")
    @Operation(description = "El Agente genera un nuevo guión")
    public ResponseEntity<ApiResponse<Story>>  generateStory(@RequestBody StoryGenerateCommand cmd) {
        return ResponseEntity.ok(ApiResponse.ok(storyGenerator.generate(cmd)));
    }

    @GetMapping("/story/{id}/audio")
    public ResponseEntity<AudioStoryAgentResult> generateAudioStory(@PathVariable("id") Long id) {
        return ResponseEntity.ok(audioStoryGeneration.generateAudio(id));
    }
    

    @PostMapping("/page")
    @Operation(description = "El Agente genera una nueva página")
    public ResponseEntity<ApiResponse<StoryPage>>  generateStoryPage(@RequestBody StoryPageGenerateCommand cmd) {
        return ResponseEntity.ok(ApiResponse.ok(pageGeneration.generate(cmd)));
    }
}
