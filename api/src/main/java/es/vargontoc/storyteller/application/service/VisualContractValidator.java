package es.vargontoc.storyteller.application.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Component;

import es.vargontoc.framework.json.JsonMapperFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Valida un story.json y la imagen del personaje contra el contrato v0.1 del agente visual
 * (docs/contracts/agent-visual/input.md, seccion 5). No se detiene en el primer fallo:
 * devuelve todas las reglas incumplidas que pueda comprobar. La raiz del zip hace de {@code input/}.
 *
 * <p>No usa Validator/ValidationResult del framework: sus codigos son un enum cerrado y no
 * pueden expresar los codigos estables del contrato (CHARACTER_IMAGE_NO_ALPHA...).
 */
@Component
public class VisualContractValidator {

    public record Error(String code, String path, String message) {}

    public record Result(List<Error> errors) {
        public Result { errors = List.copyOf(errors); }
        public boolean isValid() { return errors.isEmpty(); }
    }

    private static final Pattern STORY_ID = Pattern.compile("[a-z0-9-]{3,40}");
    private static final Pattern SLUG = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final int MIN_SIDE = 1024;

    private final JsonMapper mapper = JsonMapperFactory.create();

    /**
     * @param storyJson bytes del story.json
     * @param files     ficheros del zip por ruta relativa a la raiz (p. ej. "characters/nina/character.png")
     */
    public Result validate(byte[] storyJson, Map<String, byte[]> files) {
        List<Error> errors = new ArrayList<>();
        JsonNode root = parse(storyJson, errors);
        if (root == null) {
            return new Result(errors);
        }
        checkRoot(root, errors);
        Set<String> characterIds = checkCharacters(root.get("characters"), files, errors);
        checkScenes(root.get("scenes"), characterIds, errors);
        return new Result(errors);
    }

    private JsonNode parse(byte[] bytes, List<Error> errors) {
        if (bytes == null || bytes.length == 0) {
            errors.add(new Error("STORY_JSON_INVALID", "$", "El story.json esta vacio."));
            return null;
        }
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
            errors.add(new Error("STORY_JSON_INVALID", "$", "El story.json debe ser UTF-8 sin BOM."));
            return null;
        }
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            errors.add(new Error("STORY_JSON_INVALID", "$", "El story.json no es UTF-8 valido."));
            return null;
        }
        try {
            JsonNode root = mapper.readTree(text);
            if (root == null || !root.isObject()) {
                errors.add(new Error("STORY_JSON_INVALID", "$", "La raiz del story.json debe ser un objeto."));
                return null;
            }
            return root;
        } catch (RuntimeException e) {
            errors.add(new Error("STORY_JSON_INVALID", "$",
                    "El story.json no es JSON valido o repite claves."));
            return null;
        }
    }

    private void checkRoot(JsonNode root, List<Error> errors) {
        if (!"0.1".equals(text(root, "schemaVersion"))) {
            errors.add(new Error("STORY_SCHEMA_VERSION_UNSUPPORTED", "$.schemaVersion", "schemaVersion debe ser \"0.1\"."));
        }
        String storyId = text(root, "storyId");
        if (storyId == null || !STORY_ID.matcher(storyId).matches()) {
            errors.add(new Error("STORY_ID_INVALID", "$.storyId",
                    "storyId debe tener entre 3 y 40 caracteres: minusculas ASCII, digitos y guiones."));
        }
        String title = text(root, "title");
        if (title == null || title.isBlank() || title.length() > 120) {
            errors.add(new Error("STORY_TITLE_MISSING", "$.title", "title debe tener entre 1 y 120 caracteres."));
        }
        if (!"es".equals(text(root, "language"))) {
            errors.add(new Error("STORY_LANGUAGE_INVALID", "$.language", "language debe ser \"es\"."));
        }
        if (!"3-4".equals(text(root, "ageRange"))) {
            errors.add(new Error("STORY_AGE_RANGE_INVALID", "$.ageRange", "ageRange debe ser \"3-4\"."));
        }
        checkStyle(root.get("style"), errors);
    }

    // Esta exportacion solo usa la paleta base: sin colors y con paletteSource/paletteId "base".
    private void checkStyle(JsonNode style, List<Error> errors) {
        if (style == null || !style.isObject()) {
            errors.add(new Error("STORY_STYLE_INVALID", "$.style", "style es obligatorio y debe ser un objeto."));
            return;
        }
        if (!"base".equals(text(style, "paletteSource")) || !"base".equals(text(style, "paletteId"))) {
            errors.add(new Error("STORY_STYLE_INVALID", "$.style", "paletteId y paletteSource deben ser \"base\"."));
        } else if (style.has("colors")) {
            errors.add(new Error("STORY_STYLE_INVALID", "$.style.colors", "Con paleta base, colors se omite."));
        }
    }

    private Set<String> checkCharacters(JsonNode characters, Map<String, byte[]> files, List<Error> errors) {
        Set<String> ids = new HashSet<>();
        if (characters == null || !characters.isArray() || characters.isEmpty()) {
            errors.add(new Error("STORY_CHARACTERS_MISSING", "$.characters", "Debe haber al menos un personaje."));
            return ids;
        }
        int protagonists = 0;
        for (int i = 0; i < characters.size(); i++) {
            JsonNode c = characters.get(i);
            String base = "$.characters[" + i + "]";
            String id = text(c, "id");
            if (id == null || !SLUG.matcher(id).matches()) {
                errors.add(new Error("CHARACTER_ID_INVALID", base + ".id", "id debe ser un slug."));
            } else if (!ids.add(id)) {
                errors.add(new Error("CHARACTER_ID_DUPLICATE", base + ".id", "Hay dos personajes con el mismo id."));
            }
            String name = text(c, "name");
            if (name == null || name.isBlank()) {
                errors.add(new Error("CHARACTER_NAME_MISSING", base + ".name", "name es obligatorio."));
            }
            String role = text(c, "role");
            if ("protagonist".equals(role)) {
                protagonists++;
            } else if (!"supporting".equals(role)) {
                errors.add(new Error("CHARACTER_ROLE_INVALID", base + ".role", "role debe ser protagonist o supporting."));
            }
            checkTraits(c.get("immutableTraits"), base, errors);
            checkImage(text(c, "image"), base + ".image", files, errors);
        }
        if (protagonists != 1) {
            errors.add(new Error("STORY_CHARACTERS_MISSING", "$.characters", "Debe haber exactamente un protagonist."));
        }
        return ids;
    }

    private void checkTraits(JsonNode traits, String base, List<Error> errors) {
        boolean ok = traits != null && traits.isArray() && traits.size() >= 1 && traits.size() <= 5;
        if (ok) {
            for (JsonNode t : traits) {
                if (!t.isString() || t.asString().isBlank()) {
                    ok = false;
                }
            }
        }
        if (!ok) {
            errors.add(new Error("CHARACTER_TRAITS_INVALID", base + ".immutableTraits",
                    "immutableTraits debe tener entre 1 y 5 rasgos no vacios."));
        }
    }

    private void checkImage(String image, String path, Map<String, byte[]> files, List<Error> errors) {
        byte[] content = resolve(image, files);
        if (content == null) {
            errors.add(new Error("CHARACTER_IMAGE_MISSING", path, "La imagen no existe en input/ o su ruta no es relativa y contenida."));
            return;
        }
        if (!isPng(content)) {
            errors.add(new Error("CHARACTER_IMAGE_NOT_PNG", path, "El archivo no es un PNG real."));
            return;
        }
        BufferedImage img;
        try {
            img = ImageIO.read(new ByteArrayInputStream(content));
        } catch (IOException e) {
            img = null;
        }
        if (img == null) {
            errors.add(new Error("CHARACTER_IMAGE_NOT_PNG", path, "El archivo no es un PNG legible."));
            return;
        }
        if (img.getWidth() < MIN_SIDE || img.getHeight() < MIN_SIDE) {
            errors.add(new Error("CHARACTER_IMAGE_TOO_SMALL", path, "La imagen debe medir al menos 1024 x 1024 px."));
        }
        if (img.getWidth() != img.getHeight()) {
            errors.add(new Error("CHARACTER_IMAGE_NOT_SQUARE", path, "La relacion de aspecto debe ser 1:1."));
        }
        if (!hasTransparentBackground(img)) {
            errors.add(new Error("CHARACTER_IMAGE_NO_ALPHA", path, "La imagen debe ser PNG con canal alfa y fondo transparente."));
        }
    }

    private byte[] resolve(String image, Map<String, byte[]> files) {
        if (image == null || image.isBlank() || files == null
                || image.startsWith("/") || image.contains("\\") || image.contains(":")) {
            return null;
        }
        for (String segment : image.split("/", -1)) {
            if (segment.isEmpty() || segment.equals("..") || segment.equals(".")) {
                return null;
            }
        }
        return files.get(image);
    }

    private boolean isPng(byte[] c) {
        if (c.length < PNG_SIGNATURE.length) {
            return false;
        }
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if (c[i] != PNG_SIGNATURE[i]) {
                return false;
            }
        }
        return true;
    }

    // Fondo transparente real: canal alfa y las cuatro esquinas casi transparentes
    // (un fondo blanco, negro o damero pintado es opaco).
    private boolean hasTransparentBackground(BufferedImage img) {
        if (!img.getColorModel().hasAlpha()) {
            return false;
        }
        int w = img.getWidth() - 1;
        int h = img.getHeight() - 1;
        int[][] corners = {{0, 0}, {w, 0}, {0, h}, {w, h}};
        for (int[] p : corners) {
            if ((img.getRGB(p[0], p[1]) >>> 24) >= 128) {
                return false;
            }
        }
        return true;
    }

    private void checkScenes(JsonNode scenes, Set<String> characterIds, List<Error> errors) {
        if (scenes == null || !scenes.isArray() || scenes.isEmpty()) {
            errors.add(new Error("STORY_SCENES_MISSING", "$.scenes", "Debe haber una escena cover y al menos una scene."));
            return;
        }
        Set<String> ids = new HashSet<>();
        boolean cover = false;
        boolean scene = false;
        for (int i = 0; i < scenes.size(); i++) {
            JsonNode s = scenes.get(i);
            String base = "$.scenes[" + i + "]";
            String id = text(s, "id");
            if (id == null || !SLUG.matcher(id).matches()) {
                errors.add(new Error("SCENE_ID_INVALID", base + ".id", "id debe ser un slug."));
            } else if (!ids.add(id)) {
                errors.add(new Error("SCENE_ID_DUPLICATE", base + ".id", "Hay dos escenas con el mismo id."));
            }
            String type = text(s, "type");
            if ("cover".equals(type)) {
                cover = true;
            } else if ("scene".equals(type)) {
                scene = true;
            } else {
                errors.add(new Error("SCENE_TYPE_INVALID", base + ".type", "type debe ser cover o scene."));
            }
            requireText(s, "text", "SCENE_TEXT_MISSING", base, errors);
            requireText(s, "visualDescription", "SCENE_DESCRIPTION_MISSING", base, errors);
            requireText(s, "setting", "SCENE_SETTING_MISSING", base, errors);
            requireText(s, "emotion", "SCENE_EMOTION_MISSING", base, errors);
            checkSceneCharacters(s.get("characters"), characterIds, base, errors);
        }
        if (!cover || !scene) {
            errors.add(new Error("STORY_SCENES_MISSING", "$.scenes", "Debe haber una escena cover y al menos una scene."));
        }
    }

    private void checkSceneCharacters(JsonNode refs, Set<String> characterIds, String base, List<Error> errors) {
        String path = base + ".characters";
        if (refs == null || !refs.isArray()) {
            errors.add(new Error("SCENE_CHARACTER_UNKNOWN", path, "characters debe ser un array de ids de personaje."));
            return;
        }
        for (JsonNode ref : refs) {
            if (!ref.isString() || !characterIds.contains(ref.asString())) {
                errors.add(new Error("SCENE_CHARACTER_UNKNOWN", path, "La escena referencia un personaje que no existe."));
                return;
            }
        }
    }

    private void requireText(JsonNode node, String field, String code, String base, List<Error> errors) {
        String value = text(node, field);
        if (value == null || value.isBlank()) {
            errors.add(new Error(code, base + "." + field, field + " es obligatorio y no puede estar vacio."));
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value != null && value.isString() ? value.asString() : null;
    }
}
