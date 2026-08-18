package es.vargontoc.storyteller.infrastructure.adapters.in.rest.web;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.vargontoc.storyteller.application.ports.in.generator.ActorGeneration;
import es.vargontoc.storyteller.application.ports.in.generator.StoryGeneration;
import es.vargontoc.storyteller.application.ports.in.generator.StoryPageGeneration;
import es.vargontoc.storyteller.domain.command.ActorReviewCommand;
import es.vargontoc.storyteller.domain.command.StoryPageReviewCommand;
import es.vargontoc.storyteller.domain.command.StoryReviewCommand;
import es.vargontoc.storyteller.domain.model.Actor;
import es.vargontoc.storyteller.domain.model.ActorReview;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.domain.model.StoryPageReview;
import es.vargontoc.storyteller.domain.model.StoryReview;
import es.vargontoc.storyteller.infrastructure.adapters.in.rest.dto.ConfirmReviewRequestDto;
import es.vargontoc.storyteller.shared.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;



@RestController
@RequestMapping("/api/v1/reviews")
@Tag(name = "Reviews")
public class ReviewsController {
    
    private final StoryGeneration storyGeneration;
    private final ActorGeneration actorGeneration;
    private final StoryPageGeneration pageGeneration;

    public ReviewsController(StoryGeneration storyGeneration, 
        ActorGeneration actorGeneration,
        StoryPageGeneration pageGeneration) {
        this.storyGeneration = storyGeneration;
        this.actorGeneration = actorGeneration;
        this.pageGeneration = pageGeneration;
    }

    @PostMapping("/story")
    @Operation(description = "El Agente genera una revisión de una guión existente según el hint del usuario")
    public ResponseEntity<ApiResponse<StoryReview>> reviewStory(@RequestBody StoryReviewCommand review) {
        return ResponseEntity.ok(ApiResponse.ok(storyGeneration.review(review)));
    }

    @GetMapping("/story/{storyId}")
    @Operation(description = "Obtiene una revisión existente de un guión")
    public ResponseEntity<ApiResponse<StoryReview>> getStoryReview(@PathVariable("storyId") Long id) {
        return ResponseEntity.ok(ApiResponse.ok(storyGeneration.getReview(id)));
    }
    
    @PostMapping("/story/confirm")
    @Operation(description = "Confirmación de la revisión existente de un guión")
    public ResponseEntity<ApiResponse<Story>> confirmReviewStory(@RequestBody ConfirmReviewRequestDto review) {
        return ResponseEntity.ok(ApiResponse.ok(storyGeneration.confirmReview(review)));
    }
    @PostMapping("/actor")
    @Operation(description = "El Agente genera una revisión de un personaje existente según el hint del usuario")
    public ResponseEntity<ApiResponse<ActorReview>> reviewActor(@RequestBody ActorReviewCommand review) {
        return ResponseEntity.ok(ApiResponse.ok(actorGeneration.review(review)));
    }
    
    @GetMapping("/actor/{actorId}")
    @Operation(description = "Obtiene una revisión existente de un personaje")
    public ResponseEntity<ApiResponse<ActorReview>> getActorReview(@PathVariable("actorId") Long id) {
        return ResponseEntity.ok(ApiResponse.ok(actorGeneration.getReview(id)));
    }
    
    @PostMapping("/actor/confirm")
    @Operation(description = "Confirmación de la revisión existente de un personaje")
    public ResponseEntity<ApiResponse<Actor>> confirmReviewActor(@RequestBody ConfirmReviewRequestDto review) {
        return ResponseEntity.ok(ApiResponse.ok(actorGeneration.confirmReview(review)));
    }

    @PostMapping("/page")
    @Operation(description = "El Agente genera una revisión de una página existente según el hint del usuario")
    public ResponseEntity<ApiResponse<StoryPageReview>> reviewPage(@RequestBody StoryPageReviewCommand review) {
        return ResponseEntity.ok(ApiResponse.ok(pageGeneration.review(review)));
    }
    
    @GetMapping("/page/{pageId}")
    @Operation(description = "Obtiene una revisión existente de una página")
    public ResponseEntity<ApiResponse<StoryPageReview>> getPageReview(@PathVariable("pageId") Long id) {
        return ResponseEntity.ok(ApiResponse.ok(pageGeneration.getReview(id)));
    }
    
    @PostMapping("/page/confirm")
    @Operation(description = "Confirmación de la revisión existente de una página")
    public ResponseEntity<ApiResponse<StoryPage>> confirmReviewPage(@RequestBody ConfirmReviewRequestDto review) {
        return ResponseEntity.ok(ApiResponse.ok(pageGeneration.confirmReview(review)));
    }

    
}
