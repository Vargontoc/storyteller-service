package es.vargontoc.storyteller.application.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import es.vargontoc.storyteller.application.ports.out.ResourceStorage;
import es.vargontoc.storyteller.domain.model.NubiGameStory;

/**
 * Compone el zip para Nubi Game: {@code story.json} con rutas explícitas relativas a la raíz,
 * la imagen de la portada (sin audio) y, por página, su imagen y su audio.
 * Cada imagen se copia tal como se subió; solo la extensión sale de su formato real.
 * El zip se arma en memoria y se devuelve: no se escribe en disco ni se persiste.
 */
@Component
public class NubiGameExporter {

    private final ResourceStorage storage;
    private final ImageContentInspector inspector;
    private final ObjectMapper mapper = new ObjectMapper();

    public NubiGameExporter(ResourceStorage storage, ImageContentInspector inspector) {
        this.storage = storage;
        this.inspector = inspector;
    }

    /** Bytes del zip. Lanza IllegalStateException si falta un fichero o una imagen no lo es (lo evalúa antes la exportación). */
    public byte[] export(NubiGameStory story) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("title", story.title());
            json.put("cover", addImage(zip, "cover", story.coverImage()));

            List<Map<String, Object>> pages = new ArrayList<>();
            for (NubiGameStory.Page page : story.pages()) {
                String dir = String.format("pages/%03d/", page.number());
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("page", page.number());
                entry.put("text", page.text());
                entry.put("image", addImage(zip, dir + "image", page.image()));
                entry.put("audio", add(zip, dir + "audio.wav", read(page.audio())));
                pages.add(entry);
            }
            json.put("pages", pages);

            add(zip, "story.json", mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(json));
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el zip del cuento", e);
        }
        return out.toByteArray();
    }

    // Copia los bytes originales; la extensión es la del formato real detectado en ellos.
    private String addImage(ZipOutputStream zip, String baseName, String path) throws IOException {
        byte[] bytes = read(path);
        String extension = inspector.identify(bytes)
            .orElseThrow(() -> new IllegalStateException("El fichero no es una imagen: " + path))
            .extension();
        return add(zip, baseName + extension, bytes);
    }

    private String add(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(bytes);
        zip.closeEntry();
        return name;
    }

    private byte[] read(String path) {
        byte[] bytes = path == null ? null : storage.getResource(path);
        if (bytes == null)
            throw new IllegalStateException("No existe el fichero: " + path);
        return bytes;
    }
}
