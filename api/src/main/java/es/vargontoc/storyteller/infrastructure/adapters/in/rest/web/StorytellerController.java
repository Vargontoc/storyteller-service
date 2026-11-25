package es.vargontoc.storyteller.infrastructure.adapters.in.rest.web;

import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.vargontoc.storyteller.application.ports.in.generator.CoverGeneration;
import es.vargontoc.storyteller.application.ports.in.generator.PageAcceptance;
import es.vargontoc.storyteller.application.ports.in.generator.PageGeneration;
import es.vargontoc.storyteller.application.ports.in.persistence.PageQuery;
import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.infrastructure.adapters.in.rest.dto.PageHintRequestDto;
import es.vargontoc.storyteller.domain.response.PageAccepted;
import es.vargontoc.storyteller.application.ports.in.persistence.StorytellerUseCase;
import es.vargontoc.storyteller.domain.response.GenerationStarted;
import es.vargontoc.storyteller.domain.response.ProposalAccepted;
import es.vargontoc.storyteller.domain.response.ProposalsDiscarded;
import es.vargontoc.storyteller.domain.response.VersionSelected;
import es.vargontoc.storyteller.domain.model.ActorSummary;
import es.vargontoc.storyteller.domain.model.PaginatedResponse;
import es.vargontoc.storyteller.domain.model.StorySummary;
import es.vargontoc.storyteller.domain.request.PageRequest;
import es.vargontoc.storyteller.domain.request.ResourceRequest;
import es.vargontoc.storyteller.shared.ApiResponse;
import es.vargontoc.storyteller.shared.exceptions.AppException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/storyteller")
@Tag(name = "Storyteller")
public class StorytellerController {
    
    private final StorytellerUseCase useCase;
    private final CoverGeneration coverGeneration;

    private final PageGeneration pageGeneration;
    private final PageAcceptance pageAcceptance;
    private final PageQuery pageQuery;

    public StorytellerController(StorytellerUseCase useCase, CoverGeneration coverGeneration, PageGeneration pageGeneration,
            PageAcceptance pageAcceptance, PageQuery pageQuery) {
        this.useCase = useCase;
        this.coverGeneration = coverGeneration;
        this.pageGeneration = pageGeneration;
        this.pageAcceptance = pageAcceptance;
        this.pageQuery = pageQuery;
    }

    @PostMapping("/{storyId}/pages/{id}/accept")
    @Operation(description = "Acepta la portada (página 0) o una página pendiente con su versión vigente, sin imagen, y borra el histórico. No inicia ninguna generación. 409 si la vigente avisa del número de frases o hay una generación en curso del cuento")
    public ResponseEntity<ApiResponse<PageAccepted>> acceptPage(@PathVariable("storyId") Long storyId, @PathVariable("id") Long id) {
        StoryPage page = pageAcceptance.acceptPage(storyId, id);
        return ResponseEntity.ok(ApiResponse.ok(new PageAccepted(storyId, page.getId(), page.getPage(), page.getPageStatus())));
    }

    @PostMapping("/{storyId}/pages/{id}/accept-proposal")
    @Operation(description = "Acepta la propuesta vigente de la portada (página 0) o de una página aceptada reabierta. Sin confirm (o confirm=false) y con páginas posteriores no cambia nada y responde 200 con requiresConfirmation=true y affectedPages (las que se eliminarían). Con confirm=true, o sin posteriores, sustituye el contenido aceptado (sin imagen), borra su histórico y elimina las posteriores; responde 200 con requiresConfirmation=false. La portada nunca elimina páginas. 404 si la página no es del cuento; 409 si no está aceptada, no tiene propuestas, la vigente avisa del número de frases o hay una generación en curso del cuento")
    public ResponseEntity<ApiResponse<ProposalAccepted>> acceptProposal(@PathVariable("storyId") Long storyId, @PathVariable("id") Long id, @RequestParam(name = "confirm", defaultValue = "false") boolean confirm) {
        return ResponseEntity.ok(ApiResponse.ok(pageAcceptance.acceptProposal(storyId, id, confirm)));
    }

    @PostMapping("/{storyId}/pages/{id}/versions/{versionId}/select")
    @Operation(description = "Elige como vigente una versión del histórico de la portada (página 0) o de una página pendiente. Síncrono, no llama al modelo: responde 200 con la versión vigente y su aviso. 404 si la página no es del cuento o la versión no es de esa página; 409 si la página no está pendiente o hay una generación en curso del cuento")
    public ResponseEntity<ApiResponse<VersionSelected>> selectVersion(@PathVariable("storyId") Long storyId, @PathVariable("id") Long id, @PathVariable("versionId") Long versionId) {
        return ResponseEntity.ok(ApiResponse.ok(pageAcceptance.selectVersion(storyId, id, versionId)));
    }

    @PostMapping("/{storyId}/pages/{id}/proposals/discard")
    @Operation(description = "Descarta todas las propuestas de la portada (página 0) o de una página aceptada reabierta. Síncrono: conserva el contenido aceptado, su imagen y las páginas posteriores y responde 200. 404 si la página no es del cuento; 409 si la página no está aceptada, no tiene propuestas o hay una generación en curso del cuento")
    public ResponseEntity<ApiResponse<ProposalsDiscarded>> discardProposals(@PathVariable("storyId") Long storyId, @PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.ok(pageAcceptance.discardProposals(storyId, id)));
    }

    @PostMapping("/{storyId}/cover/generate")
    @Operation(description = "Inicia la generación de la escena de portada de un cuento con escaleta y guion aprobado. Responde al conceder el turno; el resultado llega por /storyteller (STARTED, VERSION_PRESENTED o FAILED con pageNumber 0)")
    public ResponseEntity<ApiResponse<GenerationStarted>> generateCover(@PathVariable("storyId") Long storyId) {
        return ResponseEntity.accepted().body(ApiResponse.ok(coverGeneration.startCover(storyId)));
    }

    @PostMapping("/{storyId}/pages/{number}/generate")
    @Operation(description = "Inicia la generación de la página N (la siguiente) de un cuento con escaleta, con la portada y las páginas anteriores aceptadas. Responde al conceder el turno; el resultado llega por /storyteller (STARTED, VERSION_PRESENTED o FAILED con pageNumber N). 409 si falta aceptar la portada o una página anterior, si N no es la siguiente o hay una generación en curso")
    public ResponseEntity<ApiResponse<GenerationStarted>> generatePage(@PathVariable("storyId") Long storyId, @PathVariable("number") int number) {
        return ResponseEntity.accepted().body(ApiResponse.ok(pageGeneration.startPage(storyId, number)));
    }

    @PostMapping("/{storyId}/pages/{id}/hint")
    @Operation(description = "Pide una nueva versión de la portada (página 0) o de una página pendiente aplicando un hint sobre su versión vigente. Responde al conceder el turno; el resultado llega por /storyteller (STARTED, VERSION_PRESENTED, HINT_REJECTED con reason o FAILED). 400 si el hint está vacío; 409 si la página no está pendiente o hay una generación en curso")
    public ResponseEntity<ApiResponse<GenerationStarted>> hintPage(@PathVariable("storyId") Long storyId, @PathVariable("id") Long id, @RequestBody PageHintRequestDto request) {
        return ResponseEntity.accepted().body(ApiResponse.ok(pageGeneration.startHint(storyId, id, request == null ? null : request.hint())));
    }

    @PostMapping("/{storyId}/pages/{id}/regenerate")
    @Operation(description = "Regenera sin hint una página pendiente cuya versión vigente tiene aviso de número de frases. Responde al conceder el turno; la nueva versión vigente se añade al histórico y el resultado llega por /storyteller (STARTED, VERSION_PRESENTED o FAILED). 409 si la vigente no avisa de frases, la página no está pendiente o hay una generación en curso")
    public ResponseEntity<ApiResponse<GenerationStarted>> regeneratePage(@PathVariable("storyId") Long storyId, @PathVariable("id") Long id) {
        return ResponseEntity.accepted().body(ApiResponse.ok(pageGeneration.startRegenerate(storyId, id)));
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
    @Operation(description = "Obtiene las paginas de un cuento. Con escaleta responde StoryPagesView: estado del cuento, generating, nextPage (0 portada, N página, null si no procede) y, por página, estado, beat, contenido aceptado (con hasImage), versiones (pendiente) o propuestas (reabierta) sin imagen y acciones calculadas. Los cuentos antiguos conservan la lista de PageSummary")
    public ResponseEntity<ApiResponse<?>> getPages(@PathVariable("storyId") Long storyId) {
        return pageQuery.findPages(storyId)
            .<ResponseEntity<ApiResponse<?>>>map(view -> ResponseEntity.ok(ApiResponse.ok(view)))
            .orElseGet(() -> ResponseEntity.ok(ApiResponse.ok(useCase.getPages(storyId))));
    }

    @GetMapping("/{storyId}/pages/{id}")
    @Operation(description = "Obtiene la página de un cuento. Con escaleta responde PageDetail con sus acciones; los cuentos antiguos conservan PageSummary")
    public ResponseEntity<ApiResponse<?>> getPage(@PathVariable("storyId") Long storyId, @PathVariable("id") Long id) {
        return pageQuery.findPage(storyId, id)
            .<ResponseEntity<ApiResponse<?>>>map(detail -> ResponseEntity.ok(ApiResponse.ok(detail)))
            .orElseGet(() -> ResponseEntity.ok(ApiResponse.ok(useCase.getPage(storyId, id))));
    }

    @GetMapping("/{storyId}/pages/{id}/image")
    @Operation(description = "Descarga la imagen importada del contenido aceptado de la página. Las versiones pendientes y las propuestas nunca tienen imagen. 404 si la página no es del cuento o no tiene imagen")
    public ResponseEntity<byte[]> getPageImage(@PathVariable("storyId") Long storyId, @PathVariable("id") Long id) {
        return ResponseEntity.ok(pageQuery.getAcceptedImage(storyId, id));
    }
    
    @PostMapping(value = "/{storyId}/pages/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(description = "Sube una imagen (parte multipart 'file') a la portada (página 0) o a una página aceptada y sustituye la anterior. Acepta cualquier formato de imagen y tamaño. 404 si la página no es del cuento; 409 si no está aceptada; 415 si el fichero no es una imagen (se conserva la imagen anterior)")
    public ResponseEntity<ApiResponse<PageAccepted>> uploadPageImage(@PathVariable("storyId") Long storyId, @PathVariable("id") Long id,
            @RequestPart(name = "file", required = false) MultipartFile file) {
        StoryPage page = pageAcceptance.uploadImage(storyId, id, bytesOf(file));
        return ResponseEntity.ok(ApiResponse.ok(new PageAccepted(storyId, page.getId(), page.getPage(), page.getPageStatus())));
    }

    private static byte[] bytesOf(MultipartFile file) {
        if (file == null || file.isEmpty())
            return new byte[0];
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new AppException("No se pudo leer el fichero subido", HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/asset")
    @Operation(description = "Obtiene el recurso almacenado segun el path")
    public ResponseEntity<byte[]> getResource(@RequestBody ResourceRequest request){
        return ResponseEntity.ok(useCase.getResource(request.path()));
    }
}
