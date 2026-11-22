package es.vargontoc.storyteller.application.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;

import org.springframework.stereotype.Component;

import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.info.MultimediaInfo;

/** Inspecciona el contenido real de un audio de voz subido (formato WAV/MP3 y duración 5..30 s). */
@Component
public class VoiceAudioInspector {

    public static final long MIN_DURATION_MS = 5_000;
    public static final long MAX_DURATION_MS = 30_000;

    private static final Set<String> ACCEPTED_FORMATS = Set.of("wav", "mp3");

    public enum Rejection { INVALID_FORMAT, INVALID_DURATION }

    /** Resultado: {@code rejection} es null si el audio es válido. */
    public record Inspection(Rejection rejection, long durationMs) {
        public boolean isValid() {
            return rejection == null;
        }
    }

    public Inspection inspect(byte[] content) {
        File temp = null;
        try {
            temp = File.createTempFile("voice-sample-", ".bin");
            Files.write(temp.toPath(), content);

            MultimediaInfo info = new MultimediaObject(temp).getInfo();
            if (info.getAudio() == null || !isAcceptedFormat(info.getFormat())) {
                return new Inspection(Rejection.INVALID_FORMAT, 0);
            }

            long duration = info.getDuration();
            boolean inRange = duration >= MIN_DURATION_MS && duration <= MAX_DURATION_MS;
            return new Inspection(inRange ? null : Rejection.INVALID_DURATION, duration);
        } catch (EncoderException e) {
            // Contenido que ffmpeg no reconoce como audio (p. ej. texto con extensión de audio).
            return new Inspection(Rejection.INVALID_FORMAT, 0);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo inspeccionar el audio: " + e.getMessage(), e);
        } finally {
            if (temp != null) {
                temp.delete();
            }
        }
    }

    // ffmpeg informa de varios nombres separados por coma (p. ej. "mov,mp4,..."): basta que alguno sea WAV/MP3.
    private boolean isAcceptedFormat(String format) {
        if (format == null) {
            return false;
        }
        for (String name : format.split(",")) {
            if (ACCEPTED_FORMATS.contains(name.trim().toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
