package es.vargontoc.storyteller.application.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.List;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.springframework.http.HttpStatus;

import es.vargontoc.storyteller.shared.exceptions.AppException;

public class AudioAssembler {
    
    public static byte[] silence(Duration duration) {
        AudioFormat format = new AudioFormat(24000, 16, 1, true, false);
        int frames = (int)(format.getFrameRate() * duration.toMillis() / 1000);
        byte[] raw = new byte[frames * format.getFrameSize()];

        try (AudioInputStream stream = new AudioInputStream(new ByteArrayInputStream(raw), format, frames)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            AudioSystem.write(stream, AudioFileFormat.Type.WAVE, out);
            return out.toByteArray();
        }catch(IOException e){
            throw new AppException("Error generando silencio: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static byte[] concat(List<byte[]> clips) {
        try {
            AudioFormat format = null;
            ByteArrayOutputStream pcm = new ByteArrayOutputStream();

            for (byte[] clip : clips) {
                try (AudioInputStream in = AudioSystem.getAudioInputStream(new ByteArrayInputStream(clip))) {
                    if (format == null)
                        format = in.getFormat();
                    in.transferTo(pcm);
                }
            }

            if (format == null)
                throw new AppException("No hay clips de audio que ensamblar", HttpStatus.BAD_REQUEST);

            byte[] pcmBytes = pcm.toByteArray();
            long frames = pcmBytes.length / format.getFrameSize();

            try (AudioInputStream appended = new AudioInputStream(new ByteArrayInputStream(pcmBytes), format, frames)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                AudioSystem.write(appended, AudioFileFormat.Type.WAVE, out);
                return out.toByteArray();
            }
        }catch(IOException | UnsupportedAudioFileException e){
            throw new AppException("Error esamblando audio: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
