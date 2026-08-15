package es.vargontoc.storyteller.infrastructure.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.springframework.stereotype.Component;

@Component
public class CharacterImageStorage {
    
    private final Path basePath = Path.of("data/character-images");

    public String save(Long characterId, byte[] imageBytes) {
        try {
            Path dir = basePath.resolve(String.valueOf(characterId));
            Files.createDirectories(dir);
            Path file = dir.resolve(Instant.now().toEpochMilli() + ".png");
            Files.write(file, imageBytes);
            return file.toString();
        }catch(IOException e){
            throw new IllegalStateException("No se pudo guardar la imagen del personaje: " + characterId, e);
        }
    }
}
