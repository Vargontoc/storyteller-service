package es.vargontoc.storyteller.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.chatterbox")
public record ChatterboxProperties(
    String baseUrl,
    long timeoutSeconds,
    String defaultVoiceName
) {
    
}
