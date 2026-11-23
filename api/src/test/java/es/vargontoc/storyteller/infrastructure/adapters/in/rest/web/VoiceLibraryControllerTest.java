package es.vargontoc.storyteller.infrastructure.adapters.in.rest.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import es.vargontoc.storyteller.application.ports.out.VoiceSampleStore;
import es.vargontoc.storyteller.application.ports.out.persistence.VoiceRepository;
import es.vargontoc.storyteller.application.service.SystemVoiceProvider;
import es.vargontoc.storyteller.application.service.VoiceAudioInspector;
import es.vargontoc.storyteller.application.service.VoiceAudioInspector.Inspection;
import es.vargontoc.storyteller.application.service.VoiceAudioInspector.Rejection;
import es.vargontoc.storyteller.application.service.VoiceLanguageCatalog;
import es.vargontoc.storyteller.application.service.VoiceLibraryService;
import es.vargontoc.storyteller.domain.model.Voice;
import es.vargontoc.storyteller.shared.exceptions.GlobalExceptionHandler;

/** TASK-027 (server): rutas REST de la biblioteca de voces sobre el servicio real y puertos simulados. */
class VoiceLibraryControllerTest {

    private static final byte[] AUDIO = {1, 2, 3};

    private VoiceRepository repository;
    private VoiceSampleStore samples;
    private VoiceAudioInspector inspector;
    private SystemVoiceProvider systemVoice;
    private List<Voice> stored;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        stored = new ArrayList<>();
        repository = mock(VoiceRepository.class);
        samples = mock(VoiceSampleStore.class);
        inspector = mock(VoiceAudioInspector.class);
        systemVoice = mock(SystemVoiceProvider.class);
        when(systemVoice.isAvailable()).thenReturn(true);
        when(systemVoice.name()).thenReturn("Narradora");
        when(inspector.inspect(any())).thenReturn(new Inspection(null, 10_000));
        when(repository.findAll()).thenAnswer(i -> new ArrayList<>(stored));
        when(repository.findByName(any())).thenAnswer(i -> stored.stream().filter(v -> v.getName().equals(i.getArgument(0))).findFirst());
        when(repository.findById(anyLong())).thenAnswer(i -> stored.stream().filter(v -> v.getId().equals(i.getArgument(0))).findFirst());
        when(repository.save(any())).thenAnswer(i -> {
            Voice voice = i.getArgument(0);
            voice.setId(100L + stored.size());
            stored.add(voice);
            return voice;
        });
        doAnswer(i -> stored.removeIf(v -> v.getId().equals(i.getArgument(0)))).when(repository).delete(anyLong());

        var service = new VoiceLibraryService(new VoiceLanguageCatalog(), inspector, repository, samples, systemVoice);
        mvc = MockMvcBuilders.standaloneSetup(new VoiceLibraryController(service, new VoiceLanguageCatalog()))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void listReturnsSystemVoiceFirstAndUploadedVoices() throws Exception {
        addVoice(7L, "Abuela", "es");
        when(systemVoice.collidesWithUploadedVoice()).thenReturn(true);

        mvc.perform(get("/api/v1/voices"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.voices.length()").value(2))
            .andExpect(jsonPath("$.data.voices[0].system").value(true))
            .andExpect(jsonPath("$.data.voices[0].id").doesNotExist())
            .andExpect(jsonPath("$.data.voices[0].name").value("Narradora"))
            .andExpect(jsonPath("$.data.voices[1].id").value(7))
            .andExpect(jsonPath("$.data.voices[1].name").value("Abuela"))
            .andExpect(jsonPath("$.data.voices[1].language").value("es"))
            .andExpect(jsonPath("$.data.voices[1].system").value(false))
            .andExpect(jsonPath("$.data.systemVoiceAvailable").value(true))
            .andExpect(jsonPath("$.data.systemVoiceNameCollision").value(true));
    }

    @Test
    void listReportsUnavailableSystemVoiceWithReason() throws Exception {
        when(systemVoice.isAvailable()).thenReturn(false);
        when(systemVoice.unavailableReason()).thenReturn("No hay voz configurada");

        mvc.perform(get("/api/v1/voices"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.voices.length()").value(0))
            .andExpect(jsonPath("$.data.systemVoiceAvailable").value(false))
            .andExpect(jsonPath("$.data.systemVoiceUnavailableReason").value("No hay voz configurada"));
    }

    @Test
    void languagesReturnCatalogWithSpanishAsDefault() throws Exception {
        mvc.perform(get("/api/v1/voices/languages"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.defaultLanguage.code").value("es"))
            .andExpect(jsonPath("$.data.defaultLanguage.name").value("Español"))
            .andExpect(jsonPath("$.data.languages[?(@.code=='en')]").exists())
            .andExpect(jsonPath("$.data.languages[?(@.code=='es')]").exists());
    }

    @Test
    void validUploadReturnsCreatedVoice() throws Exception {
        mvc.perform(upload("Abuela", "es", "true", audio()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(100))
            .andExpect(jsonPath("$.data.name").value("Abuela"))
            .andExpect(jsonPath("$.data.language").value("es"))
            .andExpect(jsonPath("$.data.system").value(false));
        verify(samples).save(100L, AUDIO);
    }

    @Test
    void uploadWithoutNameIsRejectedWithNameRequired() throws Exception {
        assertRejected(upload(null, "es", "true", audio()), 400, "NAME_REQUIRED");
    }

    @Test
    void uploadWithBlankNameIsRejectedWithNameRequired() throws Exception {
        assertRejected(upload("  ", "es", "true", audio()), 400, "NAME_REQUIRED");
    }

    @Test
    void uploadWithoutLanguageOrUnsupportedLanguageIsRejected() throws Exception {
        assertRejected(upload("Abuela", null, "true", audio()), 400, "LANGUAGE_REQUIRED");
        assertRejected(upload("Abuela", "xx", "true", audio()), 400, "LANGUAGE_REQUIRED");
    }

    @Test
    void uploadWithoutPermissionIsRejected() throws Exception {
        assertRejected(upload("Abuela", "es", null, audio()), 400, "PERMISSION_REQUIRED");
        assertRejected(upload("Abuela", "es", "false", audio()), 400, "PERMISSION_REQUIRED");
    }

    @Test
    void uploadWithRepeatedNameIsRejectedWithConflict() throws Exception {
        addVoice(7L, "Abuela", "es");
        assertRejected(upload("Abuela", "es", "true", audio()), 409, "NAME_DUPLICATED");
        assertRejected(upload("Narradora", "es", "true", audio()), 409, "NAME_DUPLICATED");
    }

    @Test
    void uploadWithInvalidFormatIsRejected() throws Exception {
        when(inspector.inspect(any())).thenReturn(new Inspection(Rejection.INVALID_FORMAT, 0));
        assertRejected(upload("Abuela", "es", "true", audio()), 400, "INVALID_FORMAT");
    }

    @Test
    void uploadWithoutFileIsRejectedAsInvalidFormat() throws Exception {
        MockMultipartHttpServletRequestBuilder request = multipart("/api/v1/voices")
            .param("name", "Abuela").param("language", "es").param("permission", "true");
        assertRejected(request, 400, "INVALID_FORMAT");
    }

    @Test
    void uploadWithInvalidDurationIsRejected() throws Exception {
        when(inspector.inspect(any())).thenReturn(new Inspection(Rejection.INVALID_DURATION, 4_000));
        assertRejected(upload("Abuela", "es", "true", audio()), 400, "INVALID_DURATION");
    }

    @Test
    void deleteUploadedVoiceRemovesItFromTheLibrary() throws Exception {
        addVoice(7L, "Abuela", "es");

        mvc.perform(delete("/api/v1/voices/7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        verify(samples).delete(7L);
        mvc.perform(get("/api/v1/voices")).andExpect(jsonPath("$.data.voices.length()").value(1));
    }

    @Test
    void deleteSystemVoiceIsRejectedWithWarning() throws Exception {
        mvc.perform(delete("/api/v1/voices/system"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").isNotEmpty())
            .andExpect(jsonPath("$.errors[0]").value("SYSTEM_VOICE_NOT_DELETABLE"));
        verify(repository, never()).delete(anyLong());
    }

    @Test
    void deleteUnknownOrMalformedIdIsNotFound() throws Exception {
        mvc.perform(delete("/api/v1/voices/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errors[0]").value("VOICE_NOT_FOUND"));
        mvc.perform(delete("/api/v1/voices/abc"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errors[0]").value("VOICE_NOT_FOUND"));
    }

    @Test
    void eligibleExcludesDeletedAndNonSpanishVoices() throws Exception {
        addVoice(7L, "Abuela", "es");
        addVoice(8L, "Grandma", "en");
        addVoice(9L, "Tío", "es");
        mvc.perform(delete("/api/v1/voices/9")).andExpect(status().isOk());

        mvc.perform(get("/api/v1/voices/eligible"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].name").value("Narradora"))
            .andExpect(jsonPath("$.data[1].id").value(7))
            .andExpect(jsonPath("$.data[1].name").value("Abuela"))
            .andExpect(jsonPath("$.data[1].sample").doesNotExist());
    }

    @Test
    void eligibleOmitsUnavailableSystemVoice() throws Exception {
        when(systemVoice.isAvailable()).thenReturn(false);
        when(systemVoice.sample()).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/voices/eligible"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(0));
    }

    private void assertRejected(MockMultipartHttpServletRequestBuilder request, int httpStatus, String reason) throws Exception {
        mvc.perform(request)
            .andExpect(status().is(httpStatus))
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").isNotEmpty())
            .andExpect(jsonPath("$.errors[0]").value(reason));
        verify(repository, never()).save(any());
        verify(samples, never()).save(anyLong(), any());
    }

    private static MockMultipartFile audio() {
        return new MockMultipartFile("file", "voz.mp3", "audio/mpeg", AUDIO);
    }

    private static MockMultipartHttpServletRequestBuilder upload(String name, String language, String permission,
            MockMultipartFile file) {
        MockMultipartHttpServletRequestBuilder request = multipart("/api/v1/voices").file(file);
        if (name != null) request.param("name", name);
        if (language != null) request.param("language", language);
        if (permission != null) request.param("permission", permission);
        return request;
    }

    private void addVoice(Long id, String name, String language) {
        Voice voice = new Voice(name, language, true);
        voice.setId(id);
        stored.add(voice);
    }
}
