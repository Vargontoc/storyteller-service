package es.vargontoc.storyteller.application.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.SequenceInputStream;
import java.time.Duration;
import java.util.ArrayList;
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
        List<AudioInputStream> streams = new ArrayList<>();

        try{
            for(byte[] clip : clips) {
                streams.add(AudioSystem.getAudioInputStream(new ByteArrayInputStream(clip)));
            }

            AudioInputStream appended = streams.get(0);
            for(int i = 1; i < streams.size(); i++) {
                appended = new AudioInputStream(new SequenceInputStream(appended, streams.get(i)), appended.getFormat(), appended.getFrameLength()   + streams.get(i).getFrameLength());
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            AudioSystem.write(appended, AudioFileFormat.Type.WAVE, out);
            return out.toByteArray();
        }catch(IOException | UnsupportedAudioFileException e){
            throw new AppException("Error esamblando audio: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
