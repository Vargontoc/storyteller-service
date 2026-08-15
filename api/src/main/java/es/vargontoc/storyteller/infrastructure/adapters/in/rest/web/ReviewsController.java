package es.vargontoc.storyteller.infrastructure.adapters.in.rest.web;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.vargontoc.storyteller.application.ports.in.generator.ActorGeneration;
import es.vargontoc.storyteller.application.ports.in.generator.StoryGeneration;
import es.vargontoc.storyteller.domain.command.ActorReviewCommand;
import es.vargontoc.storyteller.domain.command.StoryReviewCommand;
import es.vargontoc.storyteller.domain.model.Actor;
import es.vargontoc.storyteller.domain.model.ActorReview;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.model.StoryReview;
import es.vargontoc.storyteller.infrastructure.adapters.in.rest.dto.ConfirmReviewRequestDto;
import es.vargontoc.storyteller.shared.ApiResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;



@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewsController {
    
    private final StoryGeneration storyGeneration;
    private final ActorGeneration actorGeneration;

    public ReviewsController(StoryGeneration storyGeneration, ActorGeneration actorGeneration) {
        this.storyGeneration = storyGeneration;
        this.actorGeneration = actorGeneration;
    }

    @GetMapping("/story/{storyId}")
    public ResponseEntity<ApiResponse<StoryReview>> getStoryReview(@PathVariable("storyId") Long id) {
        return ResponseEntity.ok(ApiResponse.ok(storyGeneration.getReview(id)));
    }

    @PostMapping("/story")
    public ResponseEntity<ApiResponse<StoryReview>> reviewStory(@RequestBody StoryReviewCommand review) {
        return ResponseEntity.ok(ApiResponse.ok(storyGeneration.review(review)));
    }
    
    @PostMapping("/story/confirm")
    public ResponseEntity<ApiResponse<Story>> confirmReviewStory(@RequestBody ConfirmReviewRequestDto review) {
        return ResponseEntity.ok(ApiResponse.ok(storyGeneration.confirmReview(review)));
    }
    
    @GetMapping("/actor/{actorId}")
    public ResponseEntity<ApiResponse<ActorReview>> getActorReview(@PathVariable("actorId") Long id) {
        return ResponseEntity.ok(ApiResponse.ok(actorGeneration.getReview(id)));
    }

    @PostMapping("/actor")
    public ResponseEntity<ApiResponse<ActorReview>> reviewActor(@RequestBody ActorReviewCommand review) {
        return ResponseEntity.ok(ApiResponse.ok(actorGeneration.review(review)));
    }
    
    @PostMapping("/actor/confirm")
    public ResponseEntity<ApiResponse<Actor>> confirmReviewActor(@RequestBody ConfirmReviewRequestDto review) {
        return ResponseEntity.ok(ApiResponse.ok(actorGeneration.confirmReview(review)));
    }

}
