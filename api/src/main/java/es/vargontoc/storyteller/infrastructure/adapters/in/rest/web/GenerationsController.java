package es.vargontoc.storyteller.infrastructure.adapters.in.rest.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.vargontoc.storyteller.application.ports.in.generator.AudioStoryGeneration;
import es.vargontoc.storyteller.application.ports.in.generator.BeatEditing;
import es.vargontoc.storyteller.domain.command.BeatEditCommand;
import es.vargontoc.storyteller.domain.response.BeatEditResult;
import es.vargontoc.storyteller.infrastructure.adapters.in.rest.dto.BeatEditRequestDto;
import es.vargontoc.storyteller.application.ports.in.generator.StoryGeneration;
import es.vargontoc.storyteller.application.ports.in.generator.StoryPageGeneration;
import es.vargontoc.storyteller.application.ports.in.generator.StoryScriptReading;
import es.vargontoc.storyteller.domain.response.StoryApproval;
import es.vargontoc.storyteller.domain.response.StoryScript;
import es.vargontoc.storyteller.domain.command.StoryGenerateCommand;
import es.vargontoc.storyteller.domain.command.StoryPageGenerateCommand;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.domain.response.AudioStoryAgentResult;
import es.vargontoc.storyteller.shared.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;



@RestController
@RequestMapping("/api/v1/generations")
@Tag(name = "Generators")
public class GenerationsController
{
    private final StoryGeneration storyGenerator;
    private final StoryPageGeneration pageGeneration;
    private final AudioStoryGeneration audioStoryGeneration;
    private final BeatEditing beatEditing;
    private final StoryScriptReading scriptReading;

    public GenerationsController(StoryGeneration storyGenerator,
        StoryPageGeneration pageGeneration, AudioStoryGeneration audioStoryGeneration,
        BeatEditing beatEditing, StoryScriptReading scriptReading) {
        this.scriptReading = scriptReading;
        this.storyGenerator = storyGenerator;
        this.pageGeneration = pageGeneration;
        this.audioStoryGeneration = audioStoryGeneration;
        this.beatEditing = beatEditing;
    }

    @PostMapping("/story")
    @Operation(description = "El Agente genera un nuevo guión")
    public ResponseEntity<ApiResponse<Story>>  generateStory(@RequestBody StoryGenerateCommand cmd) {
        return ResponseEntity.ok(ApiResponse.ok(storyGenerator.generate(cmd)));
    }

    @PostMapping("/story/{id}/approve")
    @Operation(description = "El adulto aprueba el guión (escaleta) del cuento y se lanza la generación de la portada. La respuesta indica en coverStarted si la portada empezó; si no puede empezar la aprobación se completa igualmente")
    public ResponseEntity<ApiResponse<StoryApproval>> approveStory(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.ok(storyGenerator.approve(id)));
    }

    @GetMapping("/story/{id}/script")
    @Operation(description = "Devuelve el guión vigente del cuento (estado, versión, reparto y escaleta con page desde 1). En un cuento antiguo, scriptVersion nulo y reparto y beats vacíos")
    public ResponseEntity<ApiResponse<StoryScript>> getScript(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.ok(scriptReading.getScript(id)));
    }

    @PutMapping("/story/{id}/beat/{page}")
    @Operation(description = "Edita directamente el beat de una página interior. El director valida la edad; si hay páginas generadas responde CONFIRMATION_REQUIRED con el contador y repetir con confirmInvalidation=true las elimina y aplica (false cancela)")
    public ResponseEntity<ApiResponse<BeatEditResult>> editBeat(@PathVariable("id") Long id,
            @PathVariable("page") int page, @RequestBody BeatEditRequestDto body) {
        return ResponseEntity.ok(ApiResponse.ok(
            beatEditing.edit(new BeatEditCommand(id, page, body.toBeat(), body.confirmInvalidation()))));
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
