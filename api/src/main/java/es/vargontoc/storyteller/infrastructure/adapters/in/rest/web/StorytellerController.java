package es.vargontoc.storyteller.infrastructure.adapters.in.rest.web;

import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.vargontoc.storyteller.application.ports.in.persistence.StorytellerUseCase;
import es.vargontoc.storyteller.domain.model.ActorSummary;
import es.vargontoc.storyteller.domain.model.PageSummary;
import es.vargontoc.storyteller.domain.model.PaginatedResponse;
import es.vargontoc.storyteller.domain.model.StorySummary;
import es.vargontoc.storyteller.domain.request.PageRequest;
import es.vargontoc.storyteller.domain.request.ResourceRequest;
import es.vargontoc.storyteller.shared.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/v1/storyteller")
@Tag(name = "Storyteller")
public class StorytellerController {
    
    private final StorytellerUseCase useCase;

    public StorytellerController(StorytellerUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    @Operation(description = "Obtiene lista de cuentos paginada")
    public ResponseEntity<ApiResponse<PaginatedResponse<StorySummary>>> getStories(@ModelAttribute PageRequest request) {
        int pageSize = request.pageSize() > 0 ? request.pageSize() : 10;
        var pageable = org.springframework.data.domain.PageRequest.of(request.page(), pageSize);
        return ResponseEntity.ok(ApiResponse.ok(useCase.getStories(pageable)));
    }

    @GetMapping("/{storyId}")
    public ResponseEntity<ApiResponse<StorySummary>> getStory(@PathVariable("storyId") long id){
        return ResponseEntity.ok(ApiResponse.ok(useCase.getStory(id)));
    }
    
    @DeleteMapping("/{storyId}")
    @Operation(description =  "Elimina un cuento")
    public ResponseEntity<Void> deleteStory(@PathVariable("storyId") Long storyId){
        useCase.deleteStory(storyId);
        return ResponseEntity.ok(null);
    }


    @GetMapping("/{storyId}/actors")
    @Operation(description = "Obtiene la lista de personajes de un cuento")
    public ResponseEntity<ApiResponse<List<ActorSummary>>> getActors(@PathVariable("storyId") Long storyId) {
        return ResponseEntity.ok(ApiResponse.ok(useCase.getActors(storyId)));
    }

    @GetMapping("/{storyId}/actors/{id}")
    @Operation(description = "Obtiene el personaje de un cuento")
    public ResponseEntity<ApiResponse<ActorSummary>> getActor(@PathVariable("storyId") Long storyId, @PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.ok(useCase.getActor(storyId, id)));
    }
    
    @GetMapping("/{storyId}/pages")
    @Operation(description = "Obtiene las paginas de un cuento")
    public ResponseEntity<ApiResponse<List<PageSummary>>> getPages(@PathVariable("storyId") Long storyId) {
        return ResponseEntity.ok(ApiResponse.ok(useCase.getPages(storyId)));
    }

    @GetMapping("/{storyId}/pages/{id}")
    @Operation(description = "Obtiene la página de un cuento")
    public ResponseEntity<ApiResponse<PageSummary>> getPage(@PathVariable("storyId") Long storyId, @PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.ok(useCase.getPage(storyId, id)));
    }
    
    @PostMapping("/asset")
    @Operation(description = "Obtiene el recurso almacenado segun el path")
    public ResponseEntity<byte[]> getResource(@RequestBody ResourceRequest request){
        return ResponseEntity.ok(useCase.getResource(request.path()));
    }
}
