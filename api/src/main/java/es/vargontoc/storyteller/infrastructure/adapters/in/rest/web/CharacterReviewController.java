package es.vargontoc.storyteller.infrastructure.adapters.in.rest.web;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import es.vargontoc.storyteller.application.ports.in.character.CharacterReviewUseCase;
import es.vargontoc.storyteller.domain.command.CatalogCharacterReviewCommand;
import es.vargontoc.storyteller.domain.response.CatalogCharacterReviewRequestResult;
import es.vargontoc.storyteller.domain.response.CatalogCharacterReviewView;
import es.vargontoc.storyteller.infrastructure.adapters.in.rest.dto.ReviewCharacterRequestDto;
import es.vargontoc.storyteller.shared.ApiResponse;
import jakarta.validation.Valid;
@RestController @RequestMapping("/api/v1/catalog-characters/{catalogCharacterId}/reviews")
public class CharacterReviewController {
 private final CharacterReviewUseCase reviews;
 public CharacterReviewController(CharacterReviewUseCase reviews){this.reviews=reviews;}
 @PostMapping public ResponseEntity<ApiResponse<CatalogCharacterReviewRequestResult>> request(@PathVariable Long catalogCharacterId,@Valid @RequestBody ReviewCharacterRequestDto request){return ResponseEntity.ok(ApiResponse.ok(reviews.request(new CatalogCharacterReviewCommand(catalogCharacterId,request.target(),request.hint(),request.replacePending()))));}
 @GetMapping("/pending") public ResponseEntity<ApiResponse<CatalogCharacterReviewView>> pending(@PathVariable Long catalogCharacterId){return ResponseEntity.ok(ApiResponse.ok(reviews.getPending(catalogCharacterId)));}
}
