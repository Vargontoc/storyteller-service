package es.vargontoc.storyteller.application.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.stereotype.Component;

import es.vargontoc.framework.json.JsonMapperFactory;
import es.vargontoc.storyteller.application.ports.out.ResourceStorage;
import es.vargontoc.storyteller.application.ports.out.external.CharacterBackgroundRemover;
import es.vargontoc.storyteller.domain.model.StoryProtagonistSnapshot;
import es.vargontoc.storyteller.domain.model.VisualAgentStory;
import es.vargontoc.storyteller.domain.service.StorySlugger;
import tools.jackson.databind.json.JsonMapper;

/**
 * Compone el zip de entrada del agente visual (contrato v0.1): {@code story.json} en la raíz y
 * {@code characters/<id>/character.png} con el retrato del protagonista sin fondo.
 * Valida con {@link VisualContractValidator} antes de generar el zip. Todo en memoria: no persiste nada.
 */
@Component
public class VisualAgentExporter {

    /** Zip generado, o la lista de reglas incumplidas (sin zip). */
    public record Result(byte[] zip, List<VisualContractValidator.Error> errors) {
        public boolean isValid() {
            return zip != null;
        }
    }

    private static final int MAX_TRAITS = 5;
    private static final String PROTAGONIST_FALLBACK_ID = "protagonist";

    private final ResourceStorage storage;
    private final CharacterBackgroundRemover backgroundRemover;
    private final VisualContractValidator validator;
    private final StorySlugger slugger = new StorySlugger();
    private final JsonMapper mapper = JsonMapperFactory.create();

    public VisualAgentExporter(ResourceStorage storage, CharacterBackgroundRemover backgroundRemover,
            VisualContractValidator validator) {
        this.storage = storage;
        this.backgroundRemover = backgroundRemover;
        this.validator = validator;
    }

    public Result export(VisualAgentStory story) {
        StoryProtagonistSnapshot hero = story.protagonist();
        String heroId = slugger.slugify(hero.name());
        if (heroId.isEmpty()) {
            heroId = PROTAGONIST_FALLBACK_ID;
        }
        String imagePath = "characters/" + heroId + "/character.png";
        byte[] storyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(compose(story, heroId, imagePath));

        Map<String, byte[]> files = new LinkedHashMap<>();
        List<VisualContractValidator.Error> errors = new ArrayList<>();
        loadCharacterImage(hero, imagePath, files, errors);

        boolean imageFailed = !errors.isEmpty();
        for (VisualContractValidator.Error e : validator.validate(storyJson, files).errors()) {
            // Si la imagen no se obtuvo, la falta ya está indicada con su causa concreta.
            if (!(imageFailed && e.code().equals("CHARACTER_IMAGE_MISSING"))) {
                errors.add(e);
            }
        }
        if (!errors.isEmpty()) {
            return new Result(null, errors);
        }
        return new Result(zip(storyJson, files), List.of());
    }

    // Carga el retrato de la instantánea y le quita el fondo; si no se obtiene, añade el error y no lo incluye.
    private void loadCharacterImage(StoryProtagonistSnapshot hero, String imagePath, Map<String, byte[]> files,
            List<VisualContractValidator.Error> errors) {
        String path = "$.characters[0].image";
        byte[] portrait = hero.portraitPath() == null ? null : storage.getResource(hero.portraitPath());
        if (portrait == null) {
            errors.add(new VisualContractValidator.Error("CHARACTER_IMAGE_MISSING", path,
                    "El protagonista de la instantanea no tiene retrato."));
            return;
        }
        Optional<byte[]> cutout = backgroundRemover.removeBackground(portrait);
        if (cutout.isEmpty()) {
            errors.add(new VisualContractValidator.Error("CHARACTER_IMAGE_NOT_OBTAINED", path,
                    "No se pudo obtener la imagen del protagonista sin fondo."));
            return;
        }
        files.put(imagePath, cutout.get());
    }

    private Map<String, Object> compose(VisualAgentStory story, String heroId, String imagePath) {
        String title = clean(story.title());
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schemaVersion", "0.1");
        root.put("storyId", slugger.slugify(title));
        root.put("title", title);
        root.put("language", "es");
        root.put("ageRange", "3-4");
        root.put("style", Map.of("paletteId", "base", "paletteSource", "base"));

        Map<String, Object> hero = new LinkedHashMap<>();
        hero.put("id", heroId);
        hero.put("name", clean(story.protagonist().name()));
        hero.put("role", "protagonist");
        hero.put("image", imagePath);
        hero.put("immutableTraits", traits(story.protagonist().visualDescription()));
        root.put("characters", List.of(hero));

        List<Map<String, Object>> scenes = new ArrayList<>();
        VisualAgentStory.Cover cover = story.cover();
        scenes.add(scene("cover-01", "cover", title, cover.description(), cover.setting(), cover.tone(), heroId));
        int n = 1;
        for (VisualAgentStory.Page page : story.pages()) {
            scenes.add(scene(String.format("scene-%02d", n++), "scene",
                    page.text(), page.description(), page.setting(), page.tone(), heroId));
        }
        root.put("scenes", scenes);
        return root;
    }

    private Map<String, Object> scene(String id, String type, String text, String description, String setting,
            String tone, String heroId) {
        Map<String, Object> scene = new LinkedHashMap<>();
        scene.put("id", id);
        scene.put("type", type);
        scene.put("text", clean(text));
        scene.put("visualDescription", clean(description));
        scene.put("characters", List.of(heroId));
        scene.put("setting", clean(setting));
        scene.put("emotion", clean(tone));
        return scene;
    }

    // Rasgos literales de la descripción visual: fragmentos separados por coma, punto y coma, punto o salto de línea.
    private List<String> traits(String visualDescription) {
        List<String> traits = new ArrayList<>();
        for (String part : clean(visualDescription).split("[,;.\\n]")) {
            String trait = part.strip();
            if (!trait.isEmpty() && traits.size() < MAX_TRAITS) {
                traits.add(trait);
            }
        }
        return traits;
    }

    private String clean(String value) {
        return value == null ? "" : value.strip();
    }

    private byte[] zip(byte[] storyJson, Map<String, byte[]> files) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            add(zip, "story.json", storyJson);
            for (Map.Entry<String, byte[]> file : files.entrySet()) {
                add(zip, file.getKey(), file.getValue());
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el zip del agente visual", e);
        }
        return out.toByteArray();
    }

    private void add(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(bytes);
        zip.closeEntry();
    }
}
