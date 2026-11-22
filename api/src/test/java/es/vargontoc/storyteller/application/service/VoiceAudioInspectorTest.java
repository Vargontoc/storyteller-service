package es.vargontoc.storyteller.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import es.vargontoc.storyteller.application.service.VoiceAudioInspector.Inspection;
import es.vargontoc.storyteller.application.service.VoiceAudioInspector.Rejection;
import es.vargontoc.storyteller.application.utils.AudioAssembler;
import ws.schild.jave.Encoder;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

class VoiceAudioInspectorTest {

    @TempDir
    File dir;

    private final VoiceAudioInspector inspector = new VoiceAudioInspector();

    private byte[] wav(int seconds) {
        return AudioAssembler.silence(Duration.ofSeconds(seconds));
    }

    private byte[] convert(byte[] wav, String codec, String format) throws Exception {
        File source = new File(dir, "in.wav");
        File target = new File(dir, "out." + format);
        Files.write(source.toPath(), wav);
        AudioAttributes audio = new AudioAttributes();
        audio.setCodec(codec);
        EncodingAttributes attrs = new EncodingAttributes();
        attrs.setOutputFormat(format);
        attrs.setAudioAttributes(audio);
        new Encoder().encode(new MultimediaObject(source), target, attrs);
        return Files.readAllBytes(target.toPath());
    }

    @Test
    void acceptsWavOfTenSeconds() {
        Inspection result = inspector.inspect(wav(10));

        assertTrue(result.isValid());
        assertEquals(10_000, result.durationMs(), 100);
    }

    @Test
    void acceptsMp3OfTenSeconds() throws Exception {
        Inspection result = inspector.inspect(convert(wav(10), "libmp3lame", "mp3"));

        assertTrue(result.isValid());
        assertEquals(10_000, result.durationMs(), 200);
    }

    @Test
    void rejectsOggByFormat() throws Exception {
        Inspection result = inspector.inspect(convert(wav(10), "libvorbis", "ogg"));

        assertEquals(Rejection.INVALID_FORMAT, result.rejection());
    }

    @Test
    void rejectsTextWithAudioExtensionByFormat() {
        byte[] text = "esto no es audio, solo texto".getBytes(StandardCharsets.UTF_8);

        assertEquals(Rejection.INVALID_FORMAT, inspector.inspect(text).rejection());
    }

    @Test
    void rejectsFourSecondsByDuration() {
        assertEquals(Rejection.INVALID_DURATION, inspector.inspect(wav(4)).rejection());
    }

    @Test
    void rejectsThirtyOneSecondsByDuration() {
        assertEquals(Rejection.INVALID_DURATION, inspector.inspect(wav(31)).rejection());
    }

    @Test
    void acceptsBoundaryDurationsFiveAndThirtySeconds() {
        assertNull(inspector.inspect(wav(5)).rejection());
        assertNull(inspector.inspect(wav(30)).rejection());
    }
}
