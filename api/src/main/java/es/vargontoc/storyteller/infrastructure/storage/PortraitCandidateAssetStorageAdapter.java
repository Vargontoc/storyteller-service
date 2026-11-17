package es.vargontoc.storyteller.infrastructure.storage;

import es.vargontoc.storyteller.application.ports.out.PortraitCandidateAssetStorage;
import es.vargontoc.storyteller.shared.exceptions.AppException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class PortraitCandidateAssetStorageAdapter implements PortraitCandidateAssetStorage {
    private final Path portraitsDirectory = Path.of("characters").toAbsolutePath().normalize();

    @Override
    public void deleteCandidateAsset(String assetPath) {
        Path asset = Path.of(assetPath).toAbsolutePath().normalize();
        if (!asset.startsWith(portraitsDirectory)) {
            throw new AppException("Ruta de retrato no válida", HttpStatus.BAD_REQUEST);
        }
        try {
            Files.deleteIfExists(asset);
        } catch (IOException exception) {
            throw new AppException("No se pudo eliminar el retrato descartado", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @Override
    public String saveCandidateAsset(Long catalogCharacterId, byte[] content) {
        try {
            Path directory = portraitsDirectory.resolve(String.valueOf(catalogCharacterId));
            Files.createDirectories(directory);
            Path asset = directory.resolve(java.util.UUID.randomUUID() + ".png");
            Files.write(asset, content);
            return asset.toString();
        } catch (IOException exception) {
            throw new AppException("No se pudo guardar el retrato", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

