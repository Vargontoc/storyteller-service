package es.vargontoc.storyteller.infrastructure.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;


import es.vargontoc.storyteller.application.ports.out.ResourceStorage;
import es.vargontoc.storyteller.domain.enums.KindImage;
import es.vargontoc.storyteller.domain.enums.ResourceType;
import es.vargontoc.storyteller.domain.request.PageAssetRequest;
import es.vargontoc.storyteller.shared.exceptions.AppException;
import ws.schild.jave.Encoder;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

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

        // Borramos los assets propios
        Path dir = basePath.resolve(String.valueOf(idStory)).resolve("pages").resolve(String.valueOf(pageId));
        if(Files.exists(dir)) {
            try {
                Files.deleteIfExists(dir);
            }catch(IOException e) {
                
            }
        }

        // Borramos assets de paginas descendientes
        descendants.forEach(d ->  {
            Path desc = basePath.resolve(String.valueOf(idStory)).resolve("pages").resolve(String.valueOf(d));
            try {
                Files.deleteIfExists(desc);
            }catch(IOException e) {
                
            }
        });
    }

    @Override
    public boolean existsAsset(String asset) {
        return asset != null && !asset.isBlank() && Files.exists(Path.of(asset));
    }

    @Override
    public File downloadStory(String json, List<PageAssetRequest> assets) {
        try {
            Path zipFile = Files.createTempFile("story-", ".zip");

            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(zipFile))) {
                zip.putNextEntry(new ZipEntry("story.json"));
                zip.write(json.getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();

                for (PageAssetRequest asset : assets) {
                    Path source = Path.of(asset.asset());
                    if (!Files.exists(source))
                        continue;
                    
                    zip.putNextEntry(new ZipEntry(resourceFileName(asset)));
                    if (asset.type() == ResourceType.AUDIO)
                        zip.write(convertWavToMp3(source.toFile()));
                    else
                        Files.copy(source, zip);
                    zip.closeEntry();
                }
            }

            return zipFile.toFile();
        }catch(IOException e){
            throw new IllegalStateException("No se pudo generar el zip del cuento", e);
        }
    }

    private String resourceFileName(PageAssetRequest asset) {
        String extension = asset.type() == ResourceType.IMAGE ? ".png" : ".mp3";
        if (asset.page() == 0)
            return "cover" + extension;
        return "page_" + asset.page() + extension;
    }

    private byte[] convertWavToMp3(File source) throws IOException {
        try {
            AudioAttributes audio = new AudioAttributes();
            audio.setCodec("libmp3lame");
            audio.setBitRate(128000);
            audio.setChannels(1);
            audio.setSamplingRate(44100);

            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setOutputFormat("mp3");
            attrs.setAudioAttributes(audio);

            Encoder encoder = new Encoder();
            MultimediaObject mo = new MultimediaObject(source);
            File f = new File(source.getName() + ".mp3");
            encoder.encode(mo, f, attrs);

            return Files.readAllBytes(f.toPath());
        } catch (Exception e) {
            throw new AppException("Error conversion a mp3", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
