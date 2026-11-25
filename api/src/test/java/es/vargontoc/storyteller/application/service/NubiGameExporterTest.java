package es.vargontoc.storyteller.application.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import es.vargontoc.storyteller.application.ports.out.ResourceStorage;
import es.vargontoc.storyteller.domain.model.NubiGameStory;

class NubiGameExporterTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, byte[]> files = new HashMap<>();
    private NubiGameExporter exporter;

    @BeforeEach
    void setUp() {
        ResourceStorage storage = mock(ResourceStorage.class);
        when(storage.getResource(org.mockito.ArgumentMatchers.anyString()))
            .thenAnswer(i -> files.get(i.<String>getArgument(0)));
        exporter = new NubiGameExporter(storage, new ImageContentInspector());
    }

    private byte[] image(String format, int w, int h) throws IOException {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, format, out);
        return out.toByteArray();
    }

    private String put(String path, byte[] bytes) {
        files.put(path, bytes);
        return path;
    }

    private NubiGameStory story(String title, byte[] cover, byte[]... pageImages) {
        List<NubiGameStory.Page> pages = new ArrayList<>();
        for (int i = 0; i < pageImages.length; i++) {
            int n = i + 1;
            pages.add(new NubiGameStory.Page(n, "Texto " + n,
                put("img" + n, pageImages[i]), put("aud" + n, ("wav" + n).getBytes())));
        }
        return new NubiGameStory(title, put("cover", cover), pages);
    }

    private Map<String, byte[]> unzip(byte[] zip) throws IOException {
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry e;
            while ((e = in.getNextEntry()) != null)
                entries.put(e.getName(), in.readAllBytes());
        }
        return entries;
    }

    @Test
    void completeStoryHasStoryJsonCoverImagesAndAudiosButNoCoverAudio() throws IOException {
        byte[] png = image("png", 4, 4);
        Map<String, byte[]> zip = unzip(exporter.export(
            story("La luna dormida", png, png, png, png, png, png)));

        assertEquals(1 + 1 + 5 + 5, zip.size());
        assertTrue(zip.containsKey("story.json"));
        assertEquals(5, zip.keySet().stream().filter(n -> n.endsWith("audio.wav")).count());
        assertFalse(zip.keySet().stream().anyMatch(n -> n.startsWith("cover") && !n.endsWith(".png")));
    }

    @Test
    void storyJsonHasTitleCoverAndOrderedPagesWithResolvablePaths() throws IOException {
        byte[] png = image("png", 4, 4);
        Map<String, byte[]> zip = unzip(exporter.export(story("La luna dormida", png, png, png, png)));
        JsonNode json = mapper.readTree(zip.get("story.json"));

        assertEquals("La luna dormida", json.get("title").asText());
        assertTrue(zip.containsKey(json.get("cover").asText()));
        assertEquals(3, json.get("pages").size());
        for (int i = 0; i < 3; i++) {
            JsonNode p = json.get("pages").get(i);
            assertEquals(i + 1, p.get("page").asInt());
            assertEquals("Texto " + (i + 1), p.get("text").asText());
            assertTrue(zip.containsKey(p.get("image").asText()));
            assertTrue(zip.containsKey(p.get("audio").asText()));
            assertEquals("wav" + (i + 1), new String(zip.get(p.get("audio").asText())));
        }
    }

    @Test
    void extensionsComeFromRealFormatAndBytesAreIdentical() throws IOException {
        byte[] png = image("png", 4, 4);
        byte[] jpg = image("jpg", 4, 4);
        Map<String, byte[]> zip = unzip(exporter.export(story("T", png, jpg)));
        JsonNode json = mapper.readTree(zip.get("story.json"));

        assertTrue(json.get("cover").asText().endsWith(".png"));
        String pageImage = json.get("pages").get(0).get("image").asText();
        assertTrue(pageImage.endsWith(".jpg"));
        assertArrayEquals(jpg, zip.get(pageImage));
        assertArrayEquals(png, zip.get(json.get("cover").asText()));
    }

    @Test
    void bigBmpKeepsFormatAndDimensions() throws IOException {
        byte[] bmp = image("bmp", 4096, 3072);
        Map<String, byte[]> zip = unzip(exporter.export(story("T", image("png", 2, 2), bmp)));
        String path = mapper.readTree(zip.get("story.json")).get("pages").get(0).get("image").asText();

        assertTrue(path.endsWith(".bmp"));
        BufferedImage read = ImageIO.read(new ByteArrayInputStream(zip.get(path)));
        assertEquals(4096, read.getWidth());
        assertEquals(3072, read.getHeight());
        assertArrayEquals(bmp, zip.get(path));
    }

    @Test
    void storyJsonDoesNotDeclareImageFormat() throws IOException {
        byte[] png = image("png", 2, 2);
        JsonNode json = mapper.readTree(unzip(exporter.export(story("T", png, png))).get("story.json"));

        assertEquals(List.of("title", "cover", "pages"), iterable(json.fieldNames()));
        assertEquals(List.of("page", "text", "image", "audio"), iterable(json.get("pages").get(0).fieldNames()));
    }

    private List<String> iterable(java.util.Iterator<String> it) {
        List<String> list = new ArrayList<>();
        it.forEachRemaining(list::add);
        return list;
    }

    @Test
    void failsWhenFileIsMissingOrNotAnImage() {
        byte[] png = new byte[] { 1 };
        NubiGameStory notImage = story("T", png);
        assertThrows(IllegalStateException.class, () -> exporter.export(notImage));

        files.clear();
        assertThrows(IllegalStateException.class, () -> exporter.export(notImage));
    }

    @Test
    void doesNotChangeSourcesNorPersistAnything() throws IOException {
        byte[] png = image("png", 4, 4);
        NubiGameStory story = story("T", png, png, png);

        assertNotNull(exporter.export(story));

        // Solo se lee del almacén: ningún método de escritura y los originales intactos
        assertArrayEquals(png, files.get("img1"));
        assertEquals(5, files.size());
    }
}
