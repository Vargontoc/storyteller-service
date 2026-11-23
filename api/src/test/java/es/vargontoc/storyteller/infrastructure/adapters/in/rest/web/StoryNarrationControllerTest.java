package es.vargontoc.storyteller.infrastructure.adapters.in.rest.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import es.vargontoc.storyteller.application.ports.in.generator.StoryNarrationUseCase;
import es.vargontoc.storyteller.domain.enums.NarrationDisabledReason;
import es.vargontoc.storyteller.domain.enums.NarrationRejectionReason;
import es.vargontoc.storyteller.domain.enums.NarrationStatus;
import es.vargontoc.storyteller.domain.response.NarrationState;
import es.vargontoc.storyteller.shared.exceptions.AppException;
import es.vargontoc.storyteller.shared.exceptions.GlobalExceptionHandler;
import es.vargontoc.storyteller.shared.exceptions.NarrationRejectedException;

/** TASK-028 (server): rutas REST de la narracion sobre el caso de uso simulado. */
class StoryNarrationControllerTest {

    private StoryNarrationUseCase narration;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        narration = mock(StoryNarrationUseCase.class);
        mvc = MockMvcBuilders.standaloneSetup(new StoryNarrationController(narration))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void startWithSystemVoiceReturns202AndNarratingState() throws Exception {
        when(narration.start(7L, null)).thenReturn(new NarrationState(7L, true, false,
                NarrationDisabledReason.IN_PROGRESS, true, false, null, NarrationStatus.IN_PROGRESS));

        mvc.perform(post("/api/v1/storyteller/7/narration").contentType(MediaType.APPLICATION_JSON)
                .content("{\"voiceId\":\"system\"}"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.data.narrating").value(true))
            .andExpect(jsonPath("$.data.lastResult").value("IN_PROGRESS"));
    }

    @Test
    void startWithNumericVoicePassesItsId() throws Exception {
        when(narration.start(7L, 12L)).thenReturn(new NarrationState(7L, true, false,
                NarrationDisabledReason.IN_PROGRESS, true, false, null, NarrationStatus.IN_PROGRESS));

        mvc.perform(post("/api/v1/storyteller/7/narration").contentType(MediaType.APPLICATION_JSON)
                .content("{\"voiceId\":\"12\"}"))
            .andExpect(status().isAccepted());

        verify(narration).start(7L, 12L);
    }

    @Test
    void startWithoutValidVoiceIdIs400AndDoesNotStart() throws Exception {
        mvc.perform(post("/api/v1/storyteller/7/narration").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/storyteller/7/narration").contentType(MediaType.APPLICATION_JSON)
                .content("{\"voiceId\":\"abc\"}"))
            .andExpect(status().isBadRequest());

        verify(narration, never()).start(anyLong(), any());
    }

    @ParameterizedTest
    @EnumSource(NarrationRejectionReason.class)
    void everyRejectionIs409WithStableReasonAndMessage(NarrationRejectionReason reason) throws Exception {
        when(narration.start(7L, null)).thenThrow(new NarrationRejectedException(reason, "Motivo " + reason));

        mvc.perform(post("/api/v1/storyteller/7/narration").contentType(MediaType.APPLICATION_JSON)
                .content("{\"voiceId\":\"system\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Motivo " + reason))
            .andExpect(jsonPath("$.errors[0]").value(reason.name()));
    }

    @Test
    void stateReturnsAllFieldsAndNoScript() throws Exception {
        when(narration.getState(7L)).thenReturn(new NarrationState(7L, true, true, null, false, true, "Abuela",
                NarrationStatus.COMPLETED));

        mvc.perform(get("/api/v1/storyteller/7/narration"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.hasOutline").value(true))
            .andExpect(jsonPath("$.data.canNarrate").value(true))
            .andExpect(jsonPath("$.data.disabledReason").doesNotExist())
            .andExpect(jsonPath("$.data.narrating").value(false))
            .andExpect(jsonPath("$.data.hasAudio").value(true))
            .andExpect(jsonPath("$.data.voiceName").value("Abuela"))
            .andExpect(jsonPath("$.data.lastResult").value("COMPLETED"))
            .andExpect(jsonPath("$.data.script").doesNotExist())
            .andExpect(jsonPath("$.data.fragments").doesNotExist());
    }

    @Test
    void stateShowsDisabledReason() throws Exception {
        when(narration.getState(7L)).thenReturn(new NarrationState(7L, true, false,
                NarrationDisabledReason.PAGES_NOT_ACCEPTED, false, false, null, null));

        mvc.perform(get("/api/v1/storyteller/7/narration"))
            .andExpect(jsonPath("$.data.canNarrate").value(false))
            .andExpect(jsonPath("$.data.disabledReason").value("PAGES_NOT_ACCEPTED"));
    }

    @Test
    void pageAudioReturnsWavBytesOfThatPage() throws Exception {
        byte[] wav = {'R', 'I', 'F', 'F', 1, 2};
        when(narration.getPageAudio(7L, 2)).thenReturn(wav);

        mvc.perform(get("/api/v1/storyteller/7/pages/2/audio"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("audio/wav"))
            .andExpect(content().bytes(wav));
    }

    @Test
    void coverOrPageWithoutAudioIs404() throws Exception {
        when(narration.getPageAudio(7L, 0)).thenThrow(new AppException("AUDIO_NOT_FOUND", HttpStatus.NOT_FOUND));
        when(narration.getPageAudio(7L, 5)).thenThrow(new AppException("AUDIO_NOT_FOUND", HttpStatus.NOT_FOUND));

        mvc.perform(get("/api/v1/storyteller/7/pages/0/audio")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/storyteller/7/pages/5/audio")).andExpect(status().isNotFound());
    }
}
