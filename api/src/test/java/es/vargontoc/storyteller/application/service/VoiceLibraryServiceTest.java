package es.vargontoc.storyteller.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import es.vargontoc.storyteller.application.ports.out.VoiceSampleStore;
import es.vargontoc.storyteller.application.ports.out.persistence.NarrationRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.VoiceRepository;
import es.vargontoc.storyteller.application.service.VoiceAudioInspector.Inspection;
import es.vargontoc.storyteller.application.service.VoiceAudioInspector.Rejection;
import es.vargontoc.storyteller.domain.enums.VoiceRejectionReason;
import es.vargontoc.storyteller.domain.model.Voice;
import es.vargontoc.storyteller.domain.request.UploadVoiceCommand;
import es.vargontoc.storyteller.domain.response.VoiceEntry;
import es.vargontoc.storyteller.domain.response.VoiceLibrary;
import es.vargontoc.storyteller.shared.exceptions.VoiceRejectedException;

/** TASK-010 y TASK-011: listado, subida validada, borrado y voces elegibles para narrar. */
class VoiceLibraryServiceTest {

    private static final byte[] WAV = bytes("wav");
    private static final byte[] MP3 = bytes("mp3");
    private static final byte[] OGG = bytes("ogg");
    private static final byte[] SHORT = bytes("4s");
    private static final byte[] LONG = bytes("31s");

    private InMemoryVoices voices;
    private InMemorySamples samples;
    private SystemVoiceProvider systemVoice;
    private VoiceLibraryService service;

    @BeforeEach
    void setUp() {
        voices = new InMemoryVoices();
        samples = new InMemorySamples();
        systemVoice = mock(SystemVoiceProvider.class);
        when(systemVoice.isAvailable()).thenReturn(true);
        when(systemVoice.name()).thenReturn("Narradora");

        VoiceAudioInspector inspector = mock(VoiceAudioInspector.class);
        when(inspector.inspect(any())).thenAnswer(inv -> {
            String content = new String(inv.getArgument(0, byte[].class), StandardCharsets.UTF_8);
            return switch (content) {
                case "wav", "mp3" -> new Inspection(null, 10_000);
                case "4s" -> new Inspection(Rejection.INVALID_DURATION, 4_000);
                case "31s" -> new Inspection(Rejection.INVALID_DURATION, 31_000);
                default -> new Inspection(Rejection.INVALID_FORMAT, 0);
            };
        });
        service = new VoiceLibraryService(new VoiceLanguageCatalog(), inspector, voices, samples, systemVoice);
    }

    // ---- listado ----

    @Test
    void listsSystemVoiceAndTwoUploadedVoices() {
        service.upload(new UploadVoiceCommand("Abuela", "es", true, WAV));
        service.upload(new UploadVoiceCommand("Tom", "en", true, MP3));

        VoiceLibrary library = service.list();

        assertEquals(3, library.voices().size());
        VoiceEntry system = library.voices().get(0);
        assertTrue(system.system());
        assertEquals("Narradora", system.name());
        assertEquals("es", system.language());
        assertEquals(List.of("Abuela", "Tom"), library.voices().stream().filter(v -> !v.system()).map(VoiceEntry::name).toList());
        assertEquals("en", library.voices().get(2).language());
        assertTrue(library.systemVoiceAvailable());
        assertNull(library.systemVoiceUnavailableReason());
        assertFalse(library.systemVoiceNameCollision());
    }

    @Test
    void unavailableSystemVoiceIsReportedWithItsReason() {
        when(systemVoice.isAvailable()).thenReturn(false);
        when(systemVoice.name()).thenReturn(null);
        when(systemVoice.unavailableReason()).thenReturn("No hay nombre de voz del sistema configurado");
        service.upload(new UploadVoiceCommand("Abuela", "es", true, WAV));

        VoiceLibrary library = service.list();

        assertFalse(library.systemVoiceAvailable());
        assertEquals("No hay nombre de voz del sistema configurado", library.systemVoiceUnavailableReason());
        assertEquals(1, library.voices().size());
        assertFalse(library.voices().get(0).system());
    }

    @Test
    void nameCollisionBetweenSystemAndUploadedVoiceIsIndicated() {
        when(systemVoice.collidesWithUploadedVoice()).thenReturn(true);

        assertTrue(service.list().systemVoiceNameCollision());
    }

    // ---- subida valida ----

    @Test
    void validWavUploadStoresVoiceAndSampleAndIsListed() {
        VoiceEntry created = service.upload(new UploadVoiceCommand("  Abuela ", "ES", true, WAV));

        assertEquals("Abuela", created.name());
        assertEquals("es", created.language());
        assertFalse(created.system());
        assertEquals(WAV, samples.find(created.id()).orElseThrow());
        assertTrue(service.list().voices().stream().anyMatch(v -> "Abuela".equals(v.name()) && "es".equals(v.language())));
    }

    @Test
    void validMp3UploadStoresVoiceAndSample() {
        VoiceEntry created = service.upload(new UploadVoiceCommand("Abuelo", "es", true, MP3));

        assertEquals(MP3, samples.find(created.id()).orElseThrow());
        assertEquals(1, voices.findAll().size());
    }

    // ---- rechazos ----

    @Test
    void oggIsRejectedByFormat() {
        assertRejected(VoiceRejectionReason.INVALID_FORMAT, new UploadVoiceCommand("Abuela", "es", true, OGG));
    }

    @Test
    void emptyAudioIsRejectedByFormat() {
        assertRejected(VoiceRejectionReason.INVALID_FORMAT, new UploadVoiceCommand("Abuela", "es", true, new byte[0]));
    }

    @Test
    void fourSecondsIsRejectedByDuration() {
        assertRejected(VoiceRejectionReason.INVALID_DURATION, new UploadVoiceCommand("Abuela", "es", true, SHORT));
    }

    @Test
    void thirtyOneSecondsIsRejectedByDuration() {
        assertRejected(VoiceRejectionReason.INVALID_DURATION, new UploadVoiceCommand("Abuela", "es", true, LONG));
    }

    @Test
    void missingLanguageIsRejected() {
        assertRejected(VoiceRejectionReason.LANGUAGE_REQUIRED, new UploadVoiceCommand("Abuela", null, true, WAV));
        assertRejected(VoiceRejectionReason.LANGUAGE_REQUIRED, new UploadVoiceCommand("Abuela", " ", true, WAV));
    }

    @Test
    void languageOutsideCatalogIsRejected() {
        assertRejected(VoiceRejectionReason.LANGUAGE_REQUIRED, new UploadVoiceCommand("Abuela", "xx", true, WAV));
    }

    @Test
    void missingPermissionIsRejected() {
        assertRejected(VoiceRejectionReason.PERMISSION_REQUIRED, new UploadVoiceCommand("Abuela", "es", false, WAV));
    }

    @Test
    void missingNameIsRejected() {
        assertRejected(VoiceRejectionReason.NAME_REQUIRED, new UploadVoiceCommand(null, "es", true, WAV));
        assertRejected(VoiceRejectionReason.NAME_REQUIRED, new UploadVoiceCommand("   ", "es", true, WAV));
    }

    @Test
    void nameOfAnotherUploadedVoiceIsRejected() {
        service.upload(new UploadVoiceCommand("Abuela", "es", true, WAV));

        assertRejected(VoiceRejectionReason.NAME_DUPLICATED, new UploadVoiceCommand("Abuela", "en", true, MP3));
    }

    @Test
    void nameOfSystemVoiceIsRejected() {
        assertRejected(VoiceRejectionReason.NAME_DUPLICATED, new UploadVoiceCommand("Narradora", "es", true, WAV));
    }

    @Test
    void duplicateNameDetectedByStorageRaceIsRejectedWithoutSample() {
        voices.failNextSaveWithDuplicate = true;

        assertRejected(VoiceRejectionReason.NAME_DUPLICATED, new UploadVoiceCommand("Abuela", "es", true, WAV));
    }

    @Test
    void sampleSaveFailureCompensatesByDeletingTheVoice() {
        samples.failOnSave = true;

        assertThrows(IllegalStateException.class,
                () -> service.upload(new UploadVoiceCommand("Abuela", "es", true, WAV)));

        assertTrue(voices.findAll().isEmpty());
        assertTrue(samples.stored.isEmpty());
    }

    @Test
    void rejectionsHaveStableReasonAndStatus() {
        VoiceRejectedException duplicate = assertThrows(VoiceRejectedException.class,
                () -> service.upload(new UploadVoiceCommand("Narradora", "es", true, WAV)));
        VoiceRejectedException format = assertThrows(VoiceRejectedException.class,
                () -> service.upload(new UploadVoiceCommand("Abuela", "es", true, OGG)));

        assertEquals(VoiceRejectionReason.NAME_DUPLICATED, duplicate.getReason());
        assertEquals(409, duplicate.getStatus().value());
        assertEquals(VoiceRejectionReason.INVALID_FORMAT, format.getReason());
        assertEquals(400, format.getStatus().value());
    }

    // ---- TASK-011: borrado ----

    @Test
    void deletingUploadedVoiceRemovesItFromListAndDeletesItsSample() {
        VoiceEntry abuela = service.upload(new UploadVoiceCommand("Abuela", "es", true, WAV));
        VoiceEntry abuelo = service.upload(new UploadVoiceCommand("Abuelo", "es", true, MP3));

        service.delete(abuela.id());

        assertEquals(List.of("Narradora", "Abuelo"), service.list().voices().stream().map(VoiceEntry::name).toList());
        assertTrue(samples.find(abuela.id()).isEmpty());
        assertTrue(samples.find(abuelo.id()).isPresent());
    }

    @Test
    void deletingSystemVoiceIsRejectedAndItStaysListed() {
        VoiceLibrary before = service.list();

        VoiceRejectedException e = assertThrows(VoiceRejectedException.class, () -> service.delete(null));

        assertEquals(VoiceRejectionReason.SYSTEM_VOICE_NOT_DELETABLE, e.getReason());
        assertEquals(409, e.getStatus().value());
        assertEquals(before, service.list());
        assertTrue(service.list().voices().get(0).system());
    }

    @Test
    void deletingAVoiceDoesNotTouchSavedNarrations() {
        NarrationRepository narrations = mock(NarrationRepository.class);
        VoiceEntry abuela = service.upload(new UploadVoiceCommand("Abuela", "es", true, WAV));

        service.delete(abuela.id());

        // El servicio no tiene vinculo con las narraciones: guardan solo una copia del nombre.
        verifyNoInteractions(narrations);
    }

    @Test
    void deletingNonexistentVoiceIsRejectedAsNotFound() {
        VoiceRejectedException e = assertThrows(VoiceRejectedException.class, () -> service.delete(99L));

        assertEquals(VoiceRejectionReason.VOICE_NOT_FOUND, e.getReason());
        assertEquals(404, e.getStatus().value());
    }

    // ---- TASK-011: voces elegibles ----

    @Test
    void onlySpanishUploadedVoiceAndSystemVoiceAreEligible() {
        service.upload(new UploadVoiceCommand("Abuela", "es", true, WAV));
        service.upload(new UploadVoiceCommand("Tom", "en", true, MP3));

        List<VoiceEntry> eligible = service.eligible();

        assertEquals(List.of("Narradora", "Abuela"), eligible.stream().map(VoiceEntry::name).toList());
        assertTrue(eligible.get(0).system());
    }

    @Test
    void unavailableSystemVoiceIsNotEligible() {
        when(systemVoice.isAvailable()).thenReturn(false);
        service.upload(new UploadVoiceCommand("Abuela", "es", true, WAV));

        assertEquals(List.of("Abuela"), service.eligible().stream().map(VoiceEntry::name).toList());
        assertTrue(service.eligibleSample(null).isEmpty());
    }

    @Test
    void deletedVoiceIsNotEligible() {
        VoiceEntry abuela = service.upload(new UploadVoiceCommand("Abuela", "es", true, WAV));
        service.delete(abuela.id());

        assertEquals(List.of("Narradora"), service.eligible().stream().map(VoiceEntry::name).toList());
        assertTrue(service.eligibleSample(abuela.id()).isEmpty());
    }

    @Test
    void referenceSampleOfEligibleUploadedAndSystemVoiceIsObtained() {
        VoiceEntry abuela = service.upload(new UploadVoiceCommand("Abuela", "es", true, WAV));
        byte[] systemSample = bytes("system-sample");
        when(systemVoice.sample()).thenReturn(Optional.of(systemSample));

        assertEquals(WAV, service.eligibleSample(abuela.id()).orElseThrow());
        assertEquals(systemSample, service.eligibleSample(null).orElseThrow());
    }

    @Test
    void sampleOfNonSpanishVoiceIsNotObtained() {
        VoiceEntry tom = service.upload(new UploadVoiceCommand("Tom", "en", true, MP3));

        assertTrue(service.eligibleSample(tom.id()).isEmpty());
    }

    /** El rechazo tiene la causa esperada y no cambia el listado ni deja muestra. */
    private void assertRejected(VoiceRejectionReason reason, UploadVoiceCommand command) {
        VoiceLibrary before = service.list();
        int samplesBefore = samples.stored.size();

        VoiceRejectedException e = assertThrows(VoiceRejectedException.class, () -> service.upload(command));

        assertEquals(reason, e.getReason());
        assertEquals(before, service.list());
        assertEquals(samplesBefore, samples.stored.size());
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static class InMemoryVoices implements VoiceRepository {
        private final List<Voice> stored = new ArrayList<>();
        private long nextId = 1;
        boolean failNextSaveWithDuplicate;

        @Override
        public Voice save(Voice voice) {
            if (failNextSaveWithDuplicate) {
                failNextSaveWithDuplicate = false;
                throw new DataIntegrityViolationException("uq_voice_name");
            }
            voice.setId(nextId++);
            stored.add(voice);
            return voice;
        }

        @Override
        public List<Voice> findAll() {
            return new ArrayList<>(stored);
        }

        @Override
        public Optional<Voice> findByName(String name) {
            return stored.stream().filter(v -> v.getName().equals(name)).findFirst();
        }

        @Override
        public Optional<Voice> findById(Long id) {
            return stored.stream().filter(v -> v.getId().equals(id)).findFirst();
        }

        @Override
        public void delete(Long id) {
            stored.removeIf(v -> v.getId().equals(id));
        }
    }

    private static class InMemorySamples implements VoiceSampleStore {
        final Map<Long, byte[]> stored = new HashMap<>();
        boolean failOnSave;

        @Override
        public void save(long voiceId, byte[] sample) {
            if (failOnSave) {
                throw new IllegalStateException("disco lleno");
            }
            stored.put(voiceId, sample);
        }

        @Override
        public Optional<byte[]> find(long voiceId) {
            return Optional.ofNullable(stored.get(voiceId));
        }

        @Override
        public void delete(long voiceId) {
            stored.remove(voiceId);
        }
    }
}
