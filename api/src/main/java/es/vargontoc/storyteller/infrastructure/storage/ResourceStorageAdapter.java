package es.vargontoc.storyteller.infrastructure.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.springframework.stereotype.Component;

import es.vargontoc.storyteller.application.ports.out.ResourceStorage;
import es.vargontoc.storyteller.domain.enums.KindImage;

@Component
public class ResourceStorageAdapter implements ResourceStorage {
    
    private final Path basePath = Path.of("stories");

    @Override
    public String saveImage(Long storyId, KindImage kind, Long id, byte[] imageBytes) {
        try {
            Path dir = basePath.resolve(String.valueOf(storyId))
                .resolve(kind == KindImage.ACTOR ? "characters" : "pages")
                .resolve(String.valueOf(id));
            Files.createDirectories(dir);
            Path file = dir.resolve(Instant.now().toEpochMilli() + ".png");
            Files.write(file, imageBytes);
            return file.toString();
        }catch(IOException e){
            throw new IllegalStateException("No se pudo guardar la imagen", e);
        }
    }

    @Override
    public String saveAudio(Long storyId, Long id, byte[] audioBytes) {
        try {
            Path dir = basePath.resolve(String.valueOf(storyId))
                .resolve( "pages")
                .resolve(String.valueOf(id));
            Files.createDirectories(dir);
            Path file = dir.resolve(Instant.now().toEpochMilli() + ".wav");
            Files.write(file, audioBytes);
            return file.toString();
        }catch(IOException e) {
            throw new IllegalStateException("No se pudo guardar el audio", e);
        }
    }

    @Override
    public byte[] getResource(String path) {
        try {
            Path file = basePath.resolve(path);
            if(!Files.exists(file))
                return null;

            return Files.readAllBytes(file);
        }catch(IOException e){
            return null;
        }
    }
}
