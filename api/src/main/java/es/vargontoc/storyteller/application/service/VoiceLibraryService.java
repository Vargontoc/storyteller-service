package es.vargontoc.storyteller.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import es.vargontoc.storyteller.application.ports.in.generator.VoiceLibraryUseCase;
import es.vargontoc.storyteller.application.ports.out.VoiceSampleStore;
import es.vargontoc.storyteller.application.ports.out.persistence.VoiceRepository;
import es.vargontoc.storyteller.domain.enums.VoiceRejectionReason;
import es.vargontoc.storyteller.domain.model.Voice;
import es.vargontoc.storyteller.domain.request.UploadVoiceCommand;
import es.vargontoc.storyteller.domain.response.VoiceEntry;
import es.vargontoc.storyteller.domain.response.VoiceLibrary;
import es.vargontoc.storyteller.shared.exceptions.VoiceRejectedException;

/**
 * Biblioteca de voces: voz del sistema mas voces subidas. La subida valida todo antes de persistir; si guardar la
 * muestra falla tras el registro, se compensa borrando el registro.
 * La unicidad del nombre distingue mayusculas, igual que la base de datos (uq_voice_name).
 */
@Service
public class VoiceLibraryService implements VoiceLibraryUseCase {

    /** La voz del sistema es siempre en espanol (SUP-006). */
    private static final String SYSTEM_VOICE_LANGUAGE = "es";

    /** Solo las voces en espanol pueden narrar (REQ-020). */
    private static final String NARRATION_LANGUAGE = "es";

    private final VoiceLanguageCatalog catalog;
    private final VoiceAudioInspector inspector;
    private final VoiceRepository voices;
    private final VoiceSampleStore samples;
    private final SystemVoiceProvider systemVoice;

    public VoiceLibraryService(VoiceLanguageCatalog catalog, VoiceAudioInspector inspector, VoiceRepository voices,
            VoiceSampleStore samples, SystemVoiceProvider systemVoice) {
        this.catalog = catalog;
        this.inspector = inspector;
        this.voices = voices;
        this.samples = samples;
        this.systemVoice = systemVoice;
    }

    @Override
    public VoiceLibrary list() {
        List<VoiceEntry> entries = new ArrayList<>();
        if (systemVoice.isAvailable()) {
            entries.add(new VoiceEntry(null, systemVoice.name(), SYSTEM_VOICE_LANGUAGE, true));
        }
        voices.findAll().forEach(v -> entries.add(new VoiceEntry(v.getId(), v.getName(), v.getLanguage(), false)));
        return new VoiceLibrary(entries, systemVoice.name(), systemVoice.isAvailable(),
                systemVoice.unavailableReason(), systemVoice.collidesWithUploadedVoice());
    }

    @Override
    public VoiceEntry upload(UploadVoiceCommand command) {
        String name = command.name() == null ? "" : command.name().trim();
        if (name.isEmpty()) {
            throw reject(VoiceRejectionReason.NAME_REQUIRED, "El nombre de la voz es obligatorio");
        }
        if (!catalog.isSupported(command.language())) {
            throw reject(VoiceRejectionReason.LANGUAGE_REQUIRED, "El idioma de la voz es obligatorio y debe ser uno admitido");
        }
        if (!command.permissionDeclared()) {
            throw reject(VoiceRejectionReason.PERMISSION_REQUIRED, "Debes declarar que tienes permiso para usar el audio");
        }
        if (name.equals(systemVoice.name()) || voices.findByName(name).isPresent()) {
            throw nameDuplicated();
        }
        VoiceAudioInspector.Inspection inspection = inspect(command.audio());
        if (inspection.rejection() == VoiceAudioInspector.Rejection.INVALID_FORMAT) {
            throw reject(VoiceRejectionReason.INVALID_FORMAT, "Formato no admitido: solo WAV o MP3");
        }
        if (inspection.rejection() == VoiceAudioInspector.Rejection.INVALID_DURATION) {
            throw reject(VoiceRejectionReason.INVALID_DURATION, "La duración del audio debe estar entre 5 y 30 segundos");
        }

        Voice saved;
        try {
            saved = voices.save(new Voice(name, command.language().trim().toLowerCase(), true));
        } catch (DataIntegrityViolationException e) {
            throw nameDuplicated(); // carrera con otra subida del mismo nombre
        }
        try {
            samples.save(saved.getId(), command.audio());
        } catch (RuntimeException e) {
            voices.delete(saved.getId()); // compensacion: sin muestra no queda registro
            throw e;
        }
        return new VoiceEntry(saved.getId(), saved.getName(), saved.getLanguage(), false);
    }

    @Override
    public void delete(Long id) {
        if (id == null) {
            throw reject(VoiceRejectionReason.SYSTEM_VOICE_NOT_DELETABLE, "La voz del sistema no se puede borrar");
        }
        if (voices.findById(id).isEmpty()) {
            throw reject(VoiceRejectionReason.VOICE_NOT_FOUND, "La voz no existe");
        }
        // Sin vinculo persistente: las narraciones guardan solo una copia del nombre y no se tocan.
        voices.delete(id);
        samples.delete(id);
    }

    @Override
    public List<VoiceEntry> eligible() {
        List<VoiceEntry> entries = new ArrayList<>();
        if (systemVoice.isAvailable()) {
            entries.add(new VoiceEntry(null, systemVoice.name(), SYSTEM_VOICE_LANGUAGE, true));
        }
        voices.findAll().stream()
                .filter(v -> NARRATION_LANGUAGE.equalsIgnoreCase(v.getLanguage()))
                .forEach(v -> entries.add(new VoiceEntry(v.getId(), v.getName(), v.getLanguage(), false)));
        return entries;
    }

    @Override
    public Optional<byte[]> eligibleSample(Long id) {
        if (id == null) {
            return systemVoice.isAvailable() ? systemVoice.sample() : Optional.empty();
        }
        return voices.findById(id)
                .filter(v -> NARRATION_LANGUAGE.equalsIgnoreCase(v.getLanguage()))
                .flatMap(v -> samples.find(v.getId()));
    }

    private VoiceAudioInspector.Inspection inspect(byte[] audio) {
        if (audio == null || audio.length == 0) {
            return new VoiceAudioInspector.Inspection(VoiceAudioInspector.Rejection.INVALID_FORMAT, 0);
        }
        return inspector.inspect(audio);
    }

    private static VoiceRejectedException nameDuplicated() {
        return reject(VoiceRejectionReason.NAME_DUPLICATED, "Ya existe una voz con ese nombre");
    }

    private static VoiceRejectedException reject(VoiceRejectionReason reason, String message) {
        return new VoiceRejectedException(reason, message);
    }
}
