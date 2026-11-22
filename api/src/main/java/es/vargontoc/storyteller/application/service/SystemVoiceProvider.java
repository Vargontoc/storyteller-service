package es.vargontoc.storyteller.application.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import es.vargontoc.storyteller.application.ports.out.persistence.VoiceRepository;
import es.vargontoc.storyteller.infrastructure.config.SystemVoiceProperties;
import jakarta.annotation.PostConstruct;

/**
 * Voz del sistema configurada en la instalación. Se valida al arrancar y nunca impide el arranque:
 * si falta o es inválida, queda no disponible con su motivo.
 */
@Component
public class SystemVoiceProvider {

    private static final Logger log = LoggerFactory.getLogger(SystemVoiceProvider.class);

    private final SystemVoiceProperties properties;
    private final VoiceAudioInspector inspector;
    private final VoiceRepository voiceRepository;

    private String unavailableReason;
    private boolean collidesWithUploadedVoice;

    public SystemVoiceProvider(SystemVoiceProperties properties, VoiceAudioInspector inspector,
            VoiceRepository voiceRepository) {
        this.properties = properties;
        this.inspector = inspector;
        this.voiceRepository = voiceRepository;
    }

    @PostConstruct
    void validate() {
        unavailableReason = checkConfiguration();
        collidesWithUploadedVoice = unavailableReason == null && collides();
        if (unavailableReason != null) {
            log.warn("Voz del sistema no disponible: {}", unavailableReason);
        } else if (collidesWithUploadedVoice) {
            log.warn("El nombre de la voz del sistema '{}' coincide con el de una voz subida", name());
        }
    }

    /** Nombre configurado, o null si no hay. */
    public String name() {
        return isBlank(properties.name()) ? null : properties.name().trim();
    }

    public boolean isAvailable() {
        return unavailableReason == null;
    }

    /** Motivo de la no disponibilidad, o null si la voz está disponible. */
    public String unavailableReason() {
        return unavailableReason;
    }

    /** True si, al arrancar, el nombre coincidía con el de una voz subida. */
    public boolean collidesWithUploadedVoice() {
        return collidesWithUploadedVoice;
    }

    /** Audio de referencia de la voz; vacío si la voz no está disponible. */
    public Optional<byte[]> sample() {
        if (!isAvailable()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(Path.of(properties.sampleFile().trim())));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private String checkConfiguration() {
        if (name() == null) {
            return "No hay nombre de voz del sistema configurado";
        }
        if (isBlank(properties.sampleFile())) {
            return "No hay audio de referencia de la voz del sistema configurado";
        }
        byte[] content;
        try {
            Path path = Path.of(properties.sampleFile().trim());
            if (!Files.isRegularFile(path)) {
                return "El audio de referencia de la voz del sistema no existe: " + path;
            }
            content = Files.readAllBytes(path);
        } catch (IOException | RuntimeException e) {
            return "No se pudo leer el audio de referencia de la voz del sistema: " + e.getMessage();
        }
        try {
            VoiceAudioInspector.Inspection inspection = inspector.inspect(content);
            if (!inspection.isValid()) {
                return "El audio de referencia de la voz del sistema no es válido (" + inspection.rejection() + ")";
            }
        } catch (RuntimeException e) {
            return "No se pudo inspeccionar el audio de referencia de la voz del sistema: " + e.getMessage();
        }
        return null;
    }

    private boolean collides() {
        try {
            return voiceRepository.findByName(name()).isPresent();
        } catch (RuntimeException e) {
            log.warn("No se pudo comprobar la colisión del nombre de la voz del sistema: {}", e.getMessage());
            return false;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
