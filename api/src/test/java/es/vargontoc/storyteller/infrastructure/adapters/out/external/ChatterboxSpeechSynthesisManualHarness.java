package es.vargontoc.storyteller.infrastructure.adapters.out.external;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import es.vargontoc.storyteller.application.ports.out.external.SpeechSynthesizer;
import es.vargontoc.storyteller.domain.request.SpeechSynthesisRequest;

/**
 * Arnés de prueba manual de la síntesis contra el Chatterbox real. No forma parte de la suite: solo se
 * ejecuta con {@code -Dspeech.harness=true -Dspeech.sample=<ruta a un WAV/MP3 de 5 a 30 s>}
 * (y opcionalmente {@code -Dspeech.out=<fichero de salida>}). Necesita el contenedor de Chatterbox
 * (Multilingual V3) en {@code CHATTERBOX_URL} y la base de datos del backend (prueba de contexto).
 * Escucha el WAV generado: debe estar en español y con la voz de la muestra.
 */
@EnabledIfSystemProperty(named = "speech.harness", matches = "true")
@SpringBootTest(properties = {
        "app.agents.character-provisioning.enabled=false",
        "app.agents.director-provisioning.enabled=false",
        "app.agents.audio-narrator-provisioning.enabled=false",
        "app.portraits.reconciliation.enabled=false",
        "app.narration.reconciliation.enabled=false"})
class ChatterboxSpeechSynthesisManualHarness {

    @Autowired
    private SpeechSynthesizer synthesizer;

    @Test
    void synthesizesAShortFragment() throws Exception {
        byte[] sample = Files.readAllBytes(Path.of(System.getProperty("speech.sample")));
        Path out = Path.of(System.getProperty("speech.out", "speech-harness.wav"));

        byte[] audio = synthesizer.synthesize(new SpeechSynthesisRequest(
                "El perro dijo: ¡Guau! Y corrió hasta casa.", "es", sample, 0.7, 0.5, 0.8));

        Files.write(out, audio);
        System.out.println("WAV generado: " + out.toAbsolutePath() + " (" + audio.length + " bytes)");
    }
}
