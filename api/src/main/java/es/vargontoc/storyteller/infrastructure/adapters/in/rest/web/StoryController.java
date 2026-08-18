package es.vargontoc.storyteller.infrastructure.adapters.in.rest.web;

import org.springframework.web.bind.annotation.RequestMapping; 
import org.springframework.web.bind.annotation.RestController;

import es.vargontoc.storyteller.application.ports.in.persistence.StoryUseCase;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.shared.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;



@RestController
@RequestMapping("/api/v1/stories")
@Tag(name = "Stories")
public class StoryController {
    
    private final StoryUseCase useCase;

    public StoryController(StoryUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/{storyId}")
    @Operation(description = "Obtiene la informacion de una cuento")
    public ResponseEntity<ApiResponse<Story>> getStory(@PathVariable("storyId") Long storyId) {
        return ResponseEntity.ok(ApiResponse.ok(useCase.getStory(storyId)));
    }



}
