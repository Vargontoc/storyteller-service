package es.vargontoc.storyteller.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class VisualContractValidatorTest {

    private static final String IMAGE_PATH = "characters/nina/character.png";

    private static final String VALID_JSON = """
            {
              "schemaVersion": "0.1",
              "storyId": "la-luna-dormida",
              "title": "La luna dormida",
              "language": "es",
              "ageRange": "3-4",
              "style": { "paletteId": "base", "paletteSource": "base" },
              "characters": [
                { "id": "nina", "name": "Nina", "role": "protagonist",
                  "image": "characters/nina/character.png",
                  "immutableTraits": ["vestido amarillo", "pelo rizado"] }
              ],
              "scenes": [
                { "id": "cover-01", "type": "cover", "text": "La luna dormida",
                  "visualDescription": "Nina mira la luna.", "characters": ["nina"],
                  "setting": "habitacion", "emotion": "calma" },
                { "id": "scene-01", "type": "scene", "text": "Nina saluda a la luna.",
                  "visualDescription": "Nina en el jardin.", "characters": ["nina"],
                  "setting": "jardin", "emotion": "curiosidad" }
              ]
            }
            """;

    private final VisualContractValidator validator = new VisualContractValidator();

    @Test
    void conformInputHasNoErrors() throws IOException {
        var result = validate(VALID_JSON, png(1024, 1024, true));
        assertTrue(result.isValid(), result.errors().toString());
    }

    @Test
    void smallImageReportsTooSmall() throws IOException {
        assertEquals(List.of("CHARACTER_IMAGE_TOO_SMALL"), codes(VALID_JSON, png(768, 768, true)));
    }

    @Test
    void nonSquareImageReportsNotSquare() throws IOException {
        assertEquals(List.of("CHARACTER_IMAGE_NOT_SQUARE"), codes(VALID_JSON, png(1200, 1024, true)));
    }

    @Test
    void imageWithoutAlphaReportsNoAlpha() throws IOException {
        assertEquals(List.of("CHARACTER_IMAGE_NO_ALPHA"), codes(VALID_JSON, png(1024, 1024, false)));
    }

    @Test
    void opaqueBackgroundReportsNoAlpha() throws IOException {
        BufferedImage img = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 1024, 1024);
        g.dispose();
        assertEquals(List.of("CHARACTER_IMAGE_NO_ALPHA"), codes(VALID_JSON, write(img, "png")));
    }

    @Test
    void jpegNamedPngReportsNotPng() throws IOException {
        BufferedImage img = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_RGB);
        assertEquals(List.of("CHARACTER_IMAGE_NOT_PNG"), codes(VALID_JSON, write(img, "jpg")));
    }

    @Test
    void missingImageReportsMissing() throws IOException {
        var result = validator.validate(VALID_JSON.getBytes(StandardCharsets.UTF_8), Map.of());
        assertEquals(List.of("CHARACTER_IMAGE_MISSING"), result.errors().stream().map(VisualContractValidator.Error::code).toList());
    }

    @Test
    void imagePathOutsideInputReportsMissing() throws IOException {
        String json = VALID_JSON.replace(IMAGE_PATH, "../character.png");
        var result = validator.validate(json.getBytes(StandardCharsets.UTF_8), Map.of("../character.png", png(1024, 1024, true)));
        assertEquals("CHARACTER_IMAGE_MISSING", result.errors().get(0).code());
    }

    @Test
    void shortStoryIdReportsInvalidId() throws IOException {
        assertEquals(List.of("STORY_ID_INVALID"), codes(VALID_JSON.replace("la-luna-dormida", "ab"), png(1024, 1024, true)));
    }

    @Test
    void blankCoverDescriptionReportsMissingDescription() throws IOException {
        var result = validate(VALID_JSON.replace("Nina mira la luna.", ""), png(1024, 1024, true));
        assertEquals(1, result.errors().size());
        assertEquals("SCENE_DESCRIPTION_MISSING", result.errors().get(0).code());
        assertEquals("$.scenes[0].visualDescription", result.errors().get(0).path());
    }

    @Test
    void unknownCharacterReferenceReportsUnknown() throws IOException {
        var result = validate(VALID_JSON.replace("\"Nina en el jardin.\", \"characters\": [\"nina\"]",
                "\"Nina en el jardin.\", \"characters\": [\"fantasma\"]"), png(1024, 1024, true));
        assertEquals(List.of("SCENE_CHARACTER_UNKNOWN"), result.errors().stream().map(VisualContractValidator.Error::code).toList());
        assertEquals("$.scenes[1].characters", result.errors().get(0).path());
    }

    @Test
    void fixedFieldsReportEachError() throws IOException {
        String json = VALID_JSON.replace("\"0.1\"", "\"0.2\"").replace("\"language\": \"es\"", "\"language\": \"en\"")
                .replace("\"3-4\"", "\"5-6\"");
        assertEquals(List.of("STORY_SCHEMA_VERSION_UNSUPPORTED", "STORY_LANGUAGE_INVALID", "STORY_AGE_RANGE_INVALID"),
                codes(json, png(1024, 1024, true)));
    }

    @Test
    void styleWithColorsOrOwnPaletteReportsInvalidStyle() throws IOException {
        String withColors = VALID_JSON.replace("\"paletteSource\": \"base\" }", "\"paletteSource\": \"base\", \"colors\": [] }");
        assertEquals(List.of("STORY_STYLE_INVALID"), codes(withColors, png(1024, 1024, true)));
        String own = VALID_JSON.replace("\"paletteSource\": \"base\"", "\"paletteSource\": \"own\"");
        assertEquals(List.of("STORY_STYLE_INVALID"), codes(own, png(1024, 1024, true)));
    }

    @Test
    void bomReportsInvalidJson() throws IOException {
        byte[] body = VALID_JSON.getBytes(StandardCharsets.UTF_8);
        byte[] withBom = new byte[body.length + 3];
        withBom[0] = (byte) 0xEF;
        withBom[1] = (byte) 0xBB;
        withBom[2] = (byte) 0xBF;
        System.arraycopy(body, 0, withBom, 3, body.length);
        var result = validator.validate(withBom, Map.of(IMAGE_PATH, png(1024, 1024, true)));
        assertEquals("STORY_JSON_INVALID", result.errors().get(0).code());
    }

    @Test
    void duplicateKeysReportInvalidJson() throws IOException {
        String json = VALID_JSON.replace("\"title\": \"La luna dormida\",", "\"title\": \"La luna dormida\", \"title\": \"Otro\",");
        assertEquals(List.of("STORY_JSON_INVALID"), codes(json, png(1024, 1024, true)));
    }

    @Test
    void duplicateSceneIdsReportDuplicate() throws IOException {
        assertEquals(List.of("SCENE_ID_DUPLICATE"), codes(VALID_JSON.replace("scene-01", "cover-01"), png(1024, 1024, true)));
    }

    @Test
    void combinedFailuresAreAllReturned() throws IOException {
        var codes = codes(VALID_JSON.replace("la-luna-dormida", "ab"), png(768, 768, true));
        assertTrue(codes.containsAll(List.of("STORY_ID_INVALID", "CHARACTER_IMAGE_TOO_SMALL")), codes.toString());
        assertEquals(2, codes.size());
    }

    private VisualContractValidator.Result validate(String json, byte[] image) {
        return validator.validate(json.getBytes(StandardCharsets.UTF_8), Map.of(IMAGE_PATH, image));
    }

    private List<String> codes(String json, byte[] image) {
        return validate(json, image).errors().stream().map(VisualContractValidator.Error::code).toList();
    }

    /** PNG con esquinas transparentes (o RGB sin alfa) y un circulo opaco central. */
    private byte[] png(int w, int h, boolean alpha) throws IOException {
        BufferedImage img = new BufferedImage(w, h, alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.ORANGE);
        g.fillOval(w / 4, h / 4, w / 2, h / 2);
        g.dispose();
        return write(img, "png");
    }

    private byte[] write(BufferedImage img, String format) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, format, out);
        return out.toByteArray();
    }
}
