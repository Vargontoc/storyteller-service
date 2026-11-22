package es.vargontoc.storyteller.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Voz del sistema de la instalación. Sin valores por defecto: si falta algo, la voz no está disponible. */
@ConfigurationProperties(prefix = "app.system-voice")
public record SystemVoiceProperties(
    String name,
    String sampleFile
) {
}
