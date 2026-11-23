package es.vargontoc.storyteller.infrastructure.adapters.in.rest.web;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import es.vargontoc.storyteller.application.ports.in.generator.VoiceLibraryUseCase;
import es.vargontoc.storyteller.application.service.VoiceLanguageCatalog;
import es.vargontoc.storyteller.domain.enums.VoiceRejectionReason;
import es.vargontoc.storyteller.domain.request.UploadVoiceCommand;
import es.vargontoc.storyteller.domain.response.VoiceEntry;
import es.vargontoc.storyteller.domain.response.VoiceLibrary;
import es.vargontoc.storyteller.infrastructure.adapters.in.rest.dto.VoiceLanguagesResponse;
import es.vargontoc.storyteller.shared.ApiResponse;
import es.vargontoc.storyteller.shared.exceptions.VoiceRejectedException;

/**
 * Biblioteca de voces. Los rechazos se responden como {@code ApiResponse} de error con el motivo estable
 * ({@code VoiceRejectionReason}) en {@code errors[0]}; ver {@code GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/api/v1/voices")
public class VoiceLibraryController {

    /** Identificador reservado de la voz del sistema en la ruta de borrado; equivale a id nulo. */
    static final String SYSTEM_VOICE_ID = "system";

    private final VoiceLibraryUseCase library;
    private final VoiceLanguageCatalog languages;

    public VoiceLibraryController(VoiceLibraryUseCase library, VoiceLanguageCatalog languages) {
        this.library = library;
        this.languages = languages;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<VoiceLibrary>> list() {
        return ResponseEntity.ok(ApiResponse.ok(library.list()));
    }

    @GetMapping("/languages")
    public ResponseEntity<ApiResponse<VoiceLanguagesResponse>> languages() {
        return ResponseEntity.ok(ApiResponse.ok(new VoiceLanguagesResponse(languages.all(), languages.defaultLanguage())));
    }

    @GetMapping("/eligible")
    public ResponseEntity<ApiResponse<List<VoiceEntry>>> eligible() {
        return ResponseEntity.ok(ApiResponse.ok(library.eligible()));
    }

    /** Todos los campos son opcionales a nivel HTTP: el servicio decide el rechazo y su motivo. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VoiceEntry>> upload(
            @RequestPart(name = "file", required = false) MultipartFile file,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "language", required = false) String language,
            @RequestParam(name = "permission", defaultValue = "false") boolean permission) {
        VoiceEntry created = library.upload(new UploadVoiceCommand(name, language, permission, bytesOf(file)));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(created));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        library.delete(parseId(id));
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    private static byte[] bytesOf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return new byte[0];
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new VoiceRejectedException(VoiceRejectionReason.INVALID_FORMAT, "No se pudo leer el audio subido");
        }
    }

    private static Long parseId(String id) {
        if (SYSTEM_VOICE_ID.equals(id)) {
            return null;
        }
        try {
            return Long.valueOf(id);
        } catch (NumberFormatException e) {
            throw new VoiceRejectedException(VoiceRejectionReason.VOICE_NOT_FOUND, "La voz no existe");
        }
    }
}
