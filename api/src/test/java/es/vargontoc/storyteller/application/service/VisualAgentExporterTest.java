package es.vargontoc.storyteller.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import es.vargontoc.storyteller.application.ports.out.ResourceStorage;
import es.vargontoc.storyteller.application.ports.out.external.CharacterBackgroundRemover;
import es.vargontoc.storyteller.domain.model.StoryProtagonistSnapshot;
import es.vargontoc.storyteller.domain.model.VisualAgentStory;

class VisualAgentExporterTest {

    private static final String PORTRAIT_PATH = "stories/1/portrait";

    private final ObjectMapper mapper = new ObjectMapper();
    private ResourceStorage storage;
    private CharacterBackgroundRemover remover;
    private VisualAgentExporter exporter;
    private byte[] cutout;

    @BeforeEach
    void setUp() throws IOException {
        storage = mock(ResourceStorage.class);
        remover = mock(CharacterBackgroundRemover.class);
        when(storage.getResource(PORTRAIT_PATH)).thenReturn(new byte[] {1, 2, 3});
        cutout = transparentPng(1024);
        when(remover.removeBackground(any())).thenReturn(Optional.of(cutout));
        exporter = new VisualAgentExporter(storage, remover, new VisualContractValidator());
    }

    private byte[] transparentPng(int side) throws IOException {
        BufferedImage img = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    private VisualAgentStory story(String title, String name, String description, int pages) {
        var hero = new StoryProtagonistSnapshot(name, description, PORTRAIT_PATH);
        var cover = new VisualAgentStory.Cover("  Nina mira la luna desde la ventana ", " habitación al anochecer ", " calma ");
        var list = new java.util.ArrayList<VisualAgentStory.Page>();
        for (int i = 1; i <= pages; i++) {
            list.add(new VisualAgentStory.Page("Texto " + i, "Descripción " + i, "lugar " + i, "tono " + i));
        }
        return new VisualAgentStory(title, hero, cover, list);
    }

    private VisualAgentStory story() {
        return story("La luna dormida", "Nina", "vestido amarillo, pelo rizado, ojos grandes", 4);
    }

    private Map<String, byte[]> unzip(byte[] zip) throws IOException {
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            for (ZipEntry e = in.getNextEntry(); e != null; e = in.getNextEntry()) {
                entries.put(e.getName(), in.readAllBytes());
            }
        }
        return entries;
    }

    private JsonNode json(byte[] zip) throws IOException {
        return mapper.readTree(unzip(zip).get("story.json"));
    }

    @Test
    void zipContieneStoryJsonEnLaRaizYElRetratoSinFondo() throws IOException {
        var result = exporter.export(story());

        assertTrue(result.isValid());
        Map<String, byte[]> entries = unzip(result.zip());
        assertEquals(2, entries.size());
        assertTrue(entries.containsKey("story.json"));
        assertEquals(cutout.length, entries.get("characters/nina/character.png").length);
        assertEquals("characters/nina/character.png", json(result.zip()).at("/characters/0/image").asText());
    }

    @Test
    void camposFijosYPaletaBase() throws IOException {
        JsonNode root = json(exporter.export(story()).zip());

        assertEquals("0.1", root.get("schemaVersion").asText());
        assertEquals("es", root.get("language").asText());
        assertEquals("3-4", root.get("ageRange").asText());
        assertEquals("base", root.at("/style/paletteId").asText());
        assertEquals("base", root.at("/style/paletteSource").asText());
        assertFalse(root.get("style").has("colors"));
    }

    @Test
    void tituloYStoryIdDelSlugSonDeterministas() throws IOException {
        JsonNode first = json(exporter.export(story()).zip());
        JsonNode second = json(exporter.export(story()).zip());

        assertEquals("La luna dormida", first.get("title").asText());
        assertEquals("la-luna-dormida", first.get("storyId").asText());
        assertEquals(first, second);
    }

    @Test
    void elProtagonistaDeLaInstantaneaEsElUnicoPersonaje() throws IOException {
        JsonNode characters = json(exporter.export(story("La luna dormida", "Nina", "conejo blanco", 2)).zip())
                .get("characters");

        assertEquals(1, characters.size());
        assertEquals("protagonist", characters.get(0).get("role").asText());
        assertEquals("Nina", characters.get(0).get("name").asText());
        assertEquals("nina", characters.get(0).get("id").asText());
    }

    @Test
    void rasgosLiteralesDeLaDescripcionConMaximoCinco() throws IOException {
        String description = "vestido amarillo, pelo rizado; ojos grandes. lazo rojo, botas verdes, mochila azul";
        JsonNode traits = json(exporter.export(story("La luna dormida", "Nina", description, 2)).zip())
                .at("/characters/0/immutableTraits");

        assertEquals(5, traits.size());
        for (JsonNode t : traits) {
            assertFalse(t.asText().isBlank());
            assertTrue(description.contains(t.asText()));
        }
        assertEquals("vestido amarillo", traits.get(0).asText());
    }

    @Test
    void descripcionSinSeparadoresDaUnRasgo() throws IOException {
        JsonNode traits = json(exporter.export(story("La luna dormida", "Nina", "niña de pelo rizado", 2)).zip())
                .at("/characters/0/immutableTraits");

        assertEquals(1, traits.size());
        assertEquals("niña de pelo rizado", traits.get(0).asText());
    }

    @Test
    void laPortadaEsLaPrimeraEscenaConSuPropioLugarYTono() throws IOException {
        JsonNode cover = json(exporter.export(story()).zip()).at("/scenes/0");

        assertEquals("cover", cover.get("type").asText());
        assertEquals("cover-01", cover.get("id").asText());
        assertEquals("La luna dormida", cover.get("text").asText());
        assertEquals("Nina mira la luna desde la ventana", cover.get("visualDescription").asText());
        assertEquals("habitación al anochecer", cover.get("setting").asText());
        assertEquals("calma", cover.get("emotion").asText());
    }

    @Test
    void unaEscenaPorPaginaEnOrdenConIdsPosicionalesYSoloElProtagonista() throws IOException {
        JsonNode scenes = json(exporter.export(story()).zip()).get("scenes");

        assertEquals(5, scenes.size());
        for (int i = 1; i <= 4; i++) {
            JsonNode s = scenes.get(i);
            assertEquals("scene-0" + i, s.get("id").asText());
            assertEquals("scene", s.get("type").asText());
            assertEquals("Texto " + i, s.get("text").asText());
            assertEquals("Descripción " + i, s.get("visualDescription").asText());
            assertEquals("lugar " + i, s.get("setting").asText());
            assertEquals("tono " + i, s.get("emotion").asText());
        }
        for (JsonNode s : scenes) {
            assertEquals(1, s.get("characters").size());
            assertEquals("nina", s.get("characters").get(0).asText());
        }
    }

    @Test
    void elValidadorConErroresImpideGenerarElZip() {
        VisualContractValidator validator = mock(VisualContractValidator.class);
        var error = new VisualContractValidator.Error("STORY_ID_INVALID", "$.storyId", "malo");
        when(validator.validate(any(), any())).thenReturn(new VisualContractValidator.Result(List.of(error)));
        exporter = new VisualAgentExporter(storage, remover, validator);

        var result = exporter.export(story());

        assertFalse(result.isValid());
        assertNull(result.zip());
        assertEquals(List.of(error), result.errors());
    }

    @Test
    void retratoPequenoSeRechazaConLaReglaDelValidador() throws IOException {
        when(remover.removeBackground(any())).thenReturn(Optional.of(transparentPng(768)));

        var result = exporter.export(story());

        assertNull(result.zip());
        assertTrue(result.errors().stream().anyMatch(e -> e.code().equals("CHARACTER_IMAGE_TOO_SMALL")));
    }

    @Test
    void imagenNoObtenidaSeIndicaYSeAcumulanLasDemasFaltas() {
        when(remover.removeBackground(any())).thenReturn(Optional.empty());

        var result = exporter.export(story("Yo", "Nina", "pelo rizado", 2));

        assertNull(result.zip());
        List<String> codes = result.errors().stream().map(VisualContractValidator.Error::code).toList();
        assertTrue(codes.contains("CHARACTER_IMAGE_NOT_OBTAINED"));
        assertTrue(codes.contains("STORY_ID_INVALID"));
        assertFalse(codes.contains("CHARACTER_IMAGE_MISSING"));
    }

    @Test
    void sinRetratoNoSeLlamaAlEliminadorDeFondo() {
        var hero = new StoryProtagonistSnapshot("Nina", "pelo rizado", null);
        var base = story();

        var result = exporter.export(new VisualAgentStory(base.title(), hero, base.cover(), base.pages()));

        assertNull(result.zip());
        assertEquals(1, result.errors().size());
        assertEquals("CHARACTER_IMAGE_MISSING", result.errors().get(0).code());
        verify(remover, never()).removeBackground(any());
    }
}
