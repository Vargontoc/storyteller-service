package es.vargontoc.storyteller.infrastructure.adapters.in.rest.web;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import es.vargontoc.storyteller.application.ports.in.character.CharacterCatalogUseCase;
import es.vargontoc.storyteller.domain.command.CatalogCharacterCreateCommand;
import es.vargontoc.storyteller.domain.response.CatalogCharacterAgentResult;
import es.vargontoc.storyteller.infrastructure.adapters.in.rest.dto.CatalogCharacterCreateRequest;
import es.vargontoc.storyteller.infrastructure.adapters.in.rest.dto.CatalogCharacterRenameRequest;
import jakarta.validation.Valid;
import es.vargontoc.storyteller.domain.response.CatalogCharacterView;
import java.util.List;
import es.vargontoc.storyteller.shared.ApiResponse;
import es.vargontoc.storyteller.domain.model.PortraitCandidate;
@RestController @RequestMapping("/api/v1/catalog-characters")
public class CharacterCatalogController {
 private final CharacterCatalogUseCase catalog; public CharacterCatalogController(CharacterCatalogUseCase catalog){this.catalog=catalog;}
 @PostMapping public ResponseEntity<ApiResponse<CatalogCharacterAgentResult>> create(@RequestBody(required=false) CatalogCharacterCreateRequest request){
  String hint=request==null?null:request.hint(); return ResponseEntity.ok(ApiResponse.ok(catalog.create(new CatalogCharacterCreateCommand(hint)))); }
 @GetMapping public ResponseEntity<ApiResponse<List<CatalogCharacterView>>> list(){return ResponseEntity.ok(ApiResponse.ok(catalog.list()));}
 @GetMapping("/{id}") public ResponseEntity<ApiResponse<CatalogCharacterView>> get(@PathVariable Long id){return ResponseEntity.ok(ApiResponse.ok(catalog.get(id)));}
 @PatchMapping("/{id}/name") public ResponseEntity<ApiResponse<CatalogCharacterView>> rename(@PathVariable Long id,@Valid @RequestBody CatalogCharacterRenameRequest request){return ResponseEntity.ok(ApiResponse.ok(catalog.rename(id,request.name())));}
 @PostMapping("/{id}/portraits/generate") public ResponseEntity<ApiResponse<Void>> generate(@PathVariable Long id){catalog.generatePortraits(id);return ResponseEntity.ok(ApiResponse.ok(null));}
 @GetMapping("/{id}/portraits/candidates") public ResponseEntity<ApiResponse<List<PortraitCandidate>>> candidates(@PathVariable Long id){return ResponseEntity.ok(ApiResponse.ok(catalog.candidates(id)));}
 @PostMapping("/{id}/portraits/candidates/{candidateId}/select") public ResponseEntity<ApiResponse<Void>> select(@PathVariable Long id,@PathVariable Long candidateId){catalog.selectPortrait(id,candidateId);return ResponseEntity.ok(ApiResponse.ok(null));}
 @GetMapping("/{id}/portraits/candidates/{candidateId}/asset") public ResponseEntity<byte[]> candidateAsset(@PathVariable Long id,@PathVariable Long candidateId){return ResponseEntity.ok(catalog.candidateAsset(id,candidateId));}
 @GetMapping("/{id}/portraits/approved/asset") public ResponseEntity<byte[]> approvedPortraitAsset(@PathVariable Long id){return ResponseEntity.ok(catalog.approvedPortraitAsset(id));}
 @GetMapping("/{id}/portraits/generating") public ResponseEntity<ApiResponse<Boolean>> generating(@PathVariable Long id){return ResponseEntity.ok(ApiResponse.ok(catalog.generatingPortraits(id)));}
}
