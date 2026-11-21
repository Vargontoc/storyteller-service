package es.vargontoc.storyteller.infrastructure.adapters.in.rest.web;

import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.vargontoc.storyteller.application.ports.in.generator.CoverGeneration;
import es.vargontoc.storyteller.application.ports.in.generator.PageAcceptance;
import es.vargontoc.storyteller.application.ports.in.generator.PageGeneration;
import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.infrastructure.adapters.in.rest.dto.PageHintRequestDto;
import es.vargontoc.storyteller.domain.response.PageAccepted;
import es.vargontoc.storyteller.application.ports.in.persistence.StorytellerUseCase;
import es.vargontoc.storyteller.domain.response.GenerationStarted;
import es.vargontoc.storyteller.domain.model.ActorSummary;
import es.vargontoc.storyteller.domain.model.PageSummary;
import es.vargontoc.storyteller.domain.model.PaginatedResponse;
import es.vargontoc.storyteller.domain.model.StorySummary;
import es.vargontoc.storyteller.domain.request.PageRequest;
import es.vargontoc.storyteller.domain.request.ResourceRequest;
import es.vargontoc.storyteller.shared.ApiResponse;
import es.vargontoc.storyteller.shared.exceptions.AppException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    private final CoverGeneration coverGeneration;

    private final PageGeneration pageGeneration;
    private final PageAcceptance pageAcceptance;

    public StorytellerController(StorytellerUseCase useCase, CoverGeneration coverGeneration, PageGeneration pageGeneration, PageAcceptance pageAcceptance) {
        this.useCase = useCase;
        this.coverGeneration = coverGeneration;
        this.pageGeneration = pageGeneration;
        this.pageAcceptance = pageAcceptance;
    }

    @PostMapping("/{storyId}/pages/{id}/accept")
    @Operation(description = "Acepta la portada (página 0) o una página pendiente con su versión vigente, sin imagen, y borra el histórico. No inicia ninguna generación. 409 si la vigente avisa del número de frases o hay una generación en curso del cuento")
    public ResponseEntity<ApiResponse<PageAccepted>> acceptPage(@PathVariable("storyId") Long storyId, @PathVariable("id") Long id) {
        StoryPage page = pageAcceptance.acceptPage(storyId, id);
        return ResponseEntity.ok(ApiResponse.ok(new PageAccepted(storyId, page.getId(), page.getPage(), page.getPageStatus())));
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

    @GetMapping("/{storyId}/download")
    @Operation(description = "Descarga el cuento completo (json, imagenes y audios) en un zip")
    public ResponseEntity<byte[]> downloadStory(@PathVariable("storyId") Long storyId) {
        File file = useCase.downloadStory(storyId);
        try {
            byte[] content = Files.readAllBytes(file.toPath());
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"story-" + storyId + ".zip\"")
                .body(content);
        }catch(IOException e) {
            throw new AppException("No se pudo leer el zip generado: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }finally {
            try {
                Files.deleteIfExists(file.toPath());
            }catch(IOException e) {
                // Ignoramos: es un fichero temporal, no bloquea la respuesta
            }
        }
    }
}
