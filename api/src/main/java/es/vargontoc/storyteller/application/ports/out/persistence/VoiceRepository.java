package es.vargontoc.storyteller.application.ports.out.persistence;

import java.util.List;
import java.util.Optional;

import es.vargontoc.storyteller.domain.model.Voice;

public interface VoiceRepository {

    /**
     * Guarda la voz. Si ya existe otra con el mismo nombre lanza DataIntegrityViolationException
     * y la existente no cambia.
     */
    Voice save(Voice voice);

    /** Voces subidas, de la mas antigua a la mas reciente. */
    List<Voice> findAll();

    Optional<Voice> findByName(String name);

    /** Borra la voz; no toca ninguna narracion. */
    void delete(Long id);
}
