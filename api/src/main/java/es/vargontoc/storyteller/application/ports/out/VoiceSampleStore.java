package es.vargontoc.storyteller.application.ports.out;

import java.util.Optional;

/** Almacen del audio de referencia de las voces subidas, identificado por el id de la voz. */
public interface VoiceSampleStore {

    void save(long voiceId, byte[] sample);

    /** Vacio si la muestra no existe (nunca se guardo o ya se elimino). */
    Optional<byte[]> find(long voiceId);

    void delete(long voiceId);
}
