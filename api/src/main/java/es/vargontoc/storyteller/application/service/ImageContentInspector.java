package es.vargontoc.storyteller.application.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.stereotype.Component;

/**
 * Decide si unos bytes son una imagen y cuál es su formato real por su contenido (firma del formato).
 * Ignora nombre, tipo declarado, dimensiones y peso; no impone límites ni modifica los bytes.
 */
@Component
public class ImageContentInspector {

    /** Formato real de una imagen: nombre en mayúsculas (PNG, JPEG...) y extensión habitual con punto. */
    public record ImageFormat(String name, String extension) {
    }

    private static final ImageFormat WEBP = new ImageFormat("WEBP", ".webp");

    // Formatos de ImageIO (nombre del lector en minúsculas) con extensión distinta de ".<nombre>".
    private static final Map<String, String> EXTENSIONS = Map.of("jpeg", ".jpg", "tiff", ".tif");

    public boolean isImage(byte[] content) {
        return identify(content).isPresent();
    }

    /** Formato real y extensión habitual deducidos solo de los bytes; vacío si no es una imagen. */
    public Optional<ImageFormat> identify(byte[] content) {
        if (content == null || content.length == 0) {
            return Optional.empty();
        }
        if (isWebp(content)) {
            return Optional.of(WEBP);
        }
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            if (stream == null) {
                return Optional.empty();
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                return Optional.empty();
            }
            ImageReader reader = readers.next();
            try {
                String lower = reader.getFormatName().toLowerCase(Locale.ROOT);
                // Todo formato aceptado tiene extensión: la tabla o, por defecto, su propio nombre.
                return Optional.of(new ImageFormat(lower.toUpperCase(Locale.ROOT),
                        EXTENSIONS.getOrDefault(lower, "." + lower)));
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    // WebP: "RIFF" + tamaño (4 bytes) + "WEBP"; ImageIO de serie no lo lee, pero es imagen.
    private boolean isWebp(byte[] c) {
        return c.length >= 12
                && c[0] == 'R' && c[1] == 'I' && c[2] == 'F' && c[3] == 'F'
                && c[8] == 'W' && c[9] == 'E' && c[10] == 'B' && c[11] == 'P';
    }
}
