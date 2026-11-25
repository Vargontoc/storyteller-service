package es.vargontoc.storyteller.infrastructure.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

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
            clearDirectory(dir, ".png");
            Files.createDirectories(dir);
            Path file = dir.resolve(Instant.now().toEpochMilli() + ".png");
            Files.write(file, imageBytes);
            return file.toString();
        }catch(IOException e){
            throw new IllegalStateException("No se pudo guardar la imagen", e);
        }
    }

    @Override
    public String saveImportedImage(long storyId, long pageId, byte[] imageBytes) {
        try {
            Path dir = basePath.resolve(String.valueOf(storyId)).resolve("pages")
                .resolve(String.valueOf(pageId)).resolve("imported");
            Files.createDirectories(dir);
            // Sin extensión: el formato real se deduce del contenido. Primero se escribe la nueva y luego se retira la anterior
            Path file = dir.resolve(String.valueOf(System.nanoTime()));
            Files.write(file, imageBytes);
            try (var entries = Files.list(dir)) {
                for (Path entry : (Iterable<Path>) entries::iterator)
                    if (!entry.equals(file))
                        Files.deleteIfExists(entry);
            }
            return file.toString();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar la imagen importada", e);
        }
    }

    @Override
    public void deleteImportedImage(long storyId, long pageId) {
        deleteTree(basePath.resolve(String.valueOf(storyId)).resolve("pages")
            .resolve(String.valueOf(pageId)).resolve("imported"));
    }

    /** Borra el fichero o el directorio con todo su contenido; si no existe, no hace nada. */
    private void deleteTree(Path root) {
        if (!Files.exists(root))
            return;
        try (var paths = Files.walk(root)) {
            for (Path p : (Iterable<Path>) paths.sorted(java.util.Comparator.reverseOrder())::iterator)
                Files.deleteIfExists(p);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo eliminar " + root, e);
        }
    }

    @Override
    public String saveAudio(Long storyId, Long id, byte[] audioBytes) {
        try {
            Path dir = basePath.resolve(String.valueOf(storyId))
                .resolve( "pages")
                .resolve(String.valueOf(id));
            clearDirectory(dir, ".wav");
            Files.createDirectories(dir);
            Path file = dir.resolve(Instant.now().toEpochMilli() + ".wav");
            Files.write(file, audioBytes);
            return file.toString();
        }catch(IOException e) {
            throw new IllegalStateException("No se pudo guardar el audio", e);
        }
    }

    private void clearDirectory(Path dir, String extension) throws IOException {
        if (!Files.exists(dir))
            return;

        try (var entries = Files.list(dir)) {
            for (Path entry : (Iterable<Path>) entries::iterator)
                if (entry.getFileName().toString().endsWith(extension))
                    Files.deleteIfExists(entry);
        }
    }

    @Override
    public byte[] getResource(String path) {
        try {
            Path file = Path.of(path);
            if(!Files.exists(file))
                return null;

            return Files.readAllBytes(file);
        }catch(IOException e){
            return null;
        }
    }

    private Path narrationDir(long storyId) {
        return basePath.resolve(String.valueOf(storyId)).resolve("narration");
    }

    @Override
    public String saveNarrationAudio(long storyId, int page, byte[] audioBytes) {
        try {
            Path dir = narrationDir(storyId);
            Files.createDirectories(dir);
            Path file = dir.resolve(page + ".wav");
            Files.write(file, audioBytes);
            return file.toString();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el audio de la narracion", e);
        }
    }

    @Override
    public void deleteNarrationAudio(long storyId) {
        try {
            Path dir = narrationDir(storyId);
            if (!Files.exists(dir))
                return;
            clearDirectory(dir, ".wav");
            Files.deleteIfExists(dir);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo eliminar el audio de la narracion", e);
        }
    }

    private Path voiceSamplePath(long voiceId) {
        return basePath.resolve("voices").resolve(String.valueOf(voiceId)).resolve("sample");
    }

    @Override
    public void saveVoiceSample(long voiceId, byte[] sampleBytes) {
        try {
            Path file = voiceSamplePath(voiceId);
            Files.createDirectories(file.getParent());
            Files.write(file, sampleBytes);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar la muestra de voz", e);
        }
    }

    @Override
    public byte[] getVoiceSample(long voiceId) {
        return getResource(voiceSamplePath(voiceId).toString());
    }

    @Override
    public void deleteVoiceSample(long voiceId) {
        try {
            Path file = voiceSamplePath(voiceId);
            Files.deleteIfExists(file);
            Files.deleteIfExists(file.getParent());
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo eliminar la muestra de voz", e);
        }
    }

    @Override
    public void deleteStoryAssets(long idStory) {

            Path dir = basePath.resolve(String.valueOf(idStory));
            if(Files.exists(dir)) {
                try {
                    Files.deleteIfExists(dir);
                }catch(IOException e) {
                    
                }
            }

    }

    @Override
    public void deleteCharacterAssets(long idStory, long characterId) {
        // Borramos páginas
        Path dir = basePath.resolve(String.valueOf(idStory)).resolve("pages");
        if(Files.exists(dir)) {
            try {
                Files.deleteIfExists(dir);
            }catch(IOException e) {
                
            }
        }

        // Borramos assets propios del character
        dir = basePath.resolve(String.valueOf(idStory)).resolve("characters").resolve(String.valueOf(characterId));
        if(Files.exists(dir)) {
            try {
                Files.deleteIfExists(dir);
            }catch(IOException e) {
                
            }
        }

    }

    @Override
    public void deletePageAssets(long idStory, long pageId, List<Long> descendants) {

        // Borramos los assets propios y los de las páginas descendientes, con todo el contenido de sus directorios
        Path pages = basePath.resolve(String.valueOf(idStory)).resolve("pages");
        deleteTree(pages.resolve(String.valueOf(pageId)));
        descendants.forEach(d -> deleteTree(pages.resolve(String.valueOf(d))));
    }

    @Override
    public boolean existsAsset(String asset) {
        return asset != null && !asset.isBlank() && Files.exists(Path.of(asset));
    }
}
