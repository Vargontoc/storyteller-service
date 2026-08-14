package es.vargontoc.storyteller.infrastructure.web;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import es.vargontoc.storyteller.domain.RevisionStatus;
import es.vargontoc.storyteller.domain.Story;
import es.vargontoc.storyteller.domain.StoryReview;
import es.vargontoc.storyteller.infrastructure.dto.ConfirmReviewRequestDto;
import es.vargontoc.storyteller.infrastructure.dto.StoryRequestDto;
import es.vargontoc.storyteller.ports.in.StoryUseCase;
import es.vargontoc.storyteller.shared.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/v1/stories")
public class StoryController {
    
    private final StoryUseCase useCase;

    public StoryController(StoryUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping()
    public ResponseEntity<ApiResponse<Story>> generateStory(@RequestBody StoryRequestDto request) {
        return ResponseEntity.ok(ApiResponse.ok(useCase.generateStory(request)));
    }

    @GetMapping("/{storyId}")
    public ResponseEntity<ApiResponse<Story>> getStory(@PathVariable("storyId") Long storyId) {
        return ResponseEntity.ok(ApiResponse.ok(useCase.getStory(storyId)));
    }
    

    @PostMapping("/{storyId}")
    public ResponseEntity<ApiResponse<StoryReview>> reviewStory(@PathVariable("storyId") Long storyId, @NotBlank @RequestBody String hint) {

        return ResponseEntity.ok(ApiResponse.ok(useCase.reviewStory(storyId, hint)));
    }

    @GetMapping("/{storyId}/review")
    public ResponseEntity<ApiResponse<StoryReview>> getActiveReview(@PathVariable("storyId") Long storyId) {
        return ResponseEntity.ok(ApiResponse.ok(useCase.getPendingReview(storyId)));
    }

    
    @PostMapping("/{storyId}/review")
    public ResponseEntity<ApiResponse<Story>> confirmReview(@PathVariable("storyId") Long storyId, @RequestBody ConfirmReviewRequestDto request) {
        return ResponseEntity.ok(ApiResponse.ok(useCase.confirmReviewStory(storyId, request.status() == RevisionStatus.CONFIRMED)));
    }
}
