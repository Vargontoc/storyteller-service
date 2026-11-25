package es.vargontoc.storyteller.infrastructure.adapters.in.rest.web;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.vargontoc.storyteller.application.ports.in.export.StoryExport;
import es.vargontoc.storyteller.domain.response.ExportFault;
import es.vargontoc.storyteller.domain.response.ExportResult;
import es.vargontoc.storyteller.shared.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/storyteller")
@Tag(name = "Storyteller")
public class StoryExportController {

    /** Cuerpo de un rechazo: todas las faltas, cada una con su portada (page 0) o página. */
    public record ExportRejected(List<ExportFault> faults) { }

    private final StoryExport export;

    public StoryExportController(StoryExport export) {
        this.export = export;
    }

    @GetMapping("/{storyId}/export/nubi-game")
    @Operation(description = "Exporta el cuento para Nubi Game: zip con story.json, la imagen de la portada y, por página, su imagen original y su audio. 200 con el zip; 409 con ApiResponse{success:false, data:{faults:[{page (0 portada), missing (COVER_NOT_ACCEPTED, ACCEPTED_TEXT, IMAGE, AUDIO), message}]}} con todas las faltas y sin zip; 404 si el cuento no existe")
    public ResponseEntity<?> exportForNubiGame(@PathVariable("storyId") Long storyId) {
        return respond(export.exportForNubiGame(storyId), "story-" + storyId + ".zip");
    }

    @GetMapping("/{storyId}/export/visual-agent")
    @Operation(description = "Exporta el cuento para el agente visual (contrato v0.1): zip con story.json y characters/<id>/character.png, sin exigir imágenes ni audio de las páginas. 200 con el zip; 409 con ApiResponse{success:false, data:{faults:[{page (0 portada, -1 todo el cuento), missing (SCRIPT_NOT_APPROVED, COVER_NOT_ACCEPTED, COVER_SETTING_TONE, ACCEPTED_TEXT, BEAT_SETTING, PROTAGONIST_MISSING, PROTAGONIST_IMAGE, CONTRACT_RULE), message, code (regla del contrato o de la imagen; null en el resto)}]}} con todas las faltas y reglas incumplidas y sin zip; 404 si el cuento no existe")
    public ResponseEntity<?> exportForVisualAgent(@PathVariable("storyId") Long storyId) {
        return respond(export.exportForVisualAgent(storyId), "story-" + storyId + "-visual-agent.zip");
    }

    private ResponseEntity<?> respond(ExportResult result, String filename) {
        if (!result.isDelivered()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponse<>(false, new ExportRejected(result.faults()), "El cuento no se puede exportar", null));
        }
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .body(result.zip());
    }
}
