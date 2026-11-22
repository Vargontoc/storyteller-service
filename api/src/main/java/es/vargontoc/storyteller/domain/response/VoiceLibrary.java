package es.vargontoc.storyteller.domain.response;

import java.util.List;

/**
 * Biblioteca de voces.
 *
 * @param voices                     voz del sistema (solo si esta disponible, la primera) y voces subidas
 * @param systemVoiceName            nombre configurado de la voz del sistema; null si no hay
 * @param systemVoiceAvailable       la voz del sistema esta disponible
 * @param systemVoiceUnavailableReason motivo si no esta disponible; null si lo esta
 * @param systemVoiceNameCollision   el nombre de la voz del sistema coincide con el de una voz subida
 */
public record VoiceLibrary(List<VoiceEntry> voices, String systemVoiceName, boolean systemVoiceAvailable,
        String systemVoiceUnavailableReason, boolean systemVoiceNameCollision) { }
