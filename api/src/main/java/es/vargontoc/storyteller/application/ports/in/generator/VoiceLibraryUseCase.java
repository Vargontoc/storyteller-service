package es.vargontoc.storyteller.application.ports.in.generator;

import java.util.List;
import java.util.Optional;

import es.vargontoc.storyteller.domain.request.UploadVoiceCommand;
import es.vargontoc.storyteller.domain.response.VoiceEntry;
import es.vargontoc.storyteller.domain.response.VoiceLibrary;

/** Biblioteca de voces: listado y subida validada. */
public interface VoiceLibraryUseCase {

    /** Voz del sistema (con su disponibilidad) y voces subidas. */
    VoiceLibrary list();

    /**
     * Sube una voz validando todo antes de persistir.
     *
     * @throws es.vargontoc.storyteller.shared.exceptions.VoiceRejectedException con la causa del rechazo;
     *         un rechazo no deja registro ni muestra
     */
    VoiceEntry upload(UploadVoiceCommand command);

    /**
     * Borra una voz subida (registro y muestra). No modifica ninguna narración guardada.
     *
     * @param id id de la voz subida; null identifica la voz del sistema, que no se puede borrar
     * @throws es.vargontoc.storyteller.shared.exceptions.VoiceRejectedException SYSTEM_VOICE_NOT_DELETABLE
     *         o VOICE_NOT_FOUND; un rechazo no cambia nada
     */
    void delete(Long id);

    /** Voces elegibles para narrar: la del sistema si está disponible y las subidas en español. */
    List<VoiceEntry> eligible();

    /**
     * Audio de referencia de una voz elegible.
     *
     * @param id id de la voz subida; null identifica la voz del sistema
     * @return vacío si la voz no es elegible (borrada, no española, sistema no disponible) o no tiene muestra
     */
    Optional<byte[]> eligibleSample(Long id);
}
