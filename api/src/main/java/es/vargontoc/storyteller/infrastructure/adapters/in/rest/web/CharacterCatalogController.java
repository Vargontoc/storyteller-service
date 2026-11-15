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
@RestController @RequestMapping("/api/v1/catalog-characters")
public class CharacterCatalogController {
 private final CharacterCatalogUseCase catalog; public CharacterCatalogController(CharacterCatalogUseCase catalog){this.catalog=catalog;}
 @PostMapping public ResponseEntity<ApiResponse<CatalogCharacterAgentResult>> create(@RequestBody(required=false) CatalogCharacterCreateRequest request){
  String hint=request==null?null:request.hint(); return ResponseEntity.ok(ApiResponse.ok(catalog.create(new CatalogCharacterCreateCommand(hint)))); }
 @GetMapping public ResponseEntity<ApiResponse<List<CatalogCharacterView>>> list(){return ResponseEntity.ok(ApiResponse.ok(catalog.list()));}
 @GetMapping("/{id}") public ResponseEntity<ApiResponse<CatalogCharacterView>> get(@PathVariable Long id){return ResponseEntity.ok(ApiResponse.ok(catalog.get(id)));}
 @PatchMapping("/{id}/name") public ResponseEntity<ApiResponse<CatalogCharacterView>> rename(@PathVariable Long id,@Valid @RequestBody CatalogCharacterRenameRequest request){return ResponseEntity.ok(ApiResponse.ok(catalog.rename(id,request.name())));}
}
