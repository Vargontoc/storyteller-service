package es.vargontoc.storyteller.infrastructure.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import es.vargontoc.storyteller.domain.model.CharacterPortraitPrompt;
import es.vargontoc.storyteller.infrastructure.config.ComfyUIProperties;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PortraitWorkflowGraphBuilderTest {

    @Test
    void buildsFourTransparentFullBodyTemplatesFromTheEnglishPrompt() {
        var builder = new PortraitWorkflowGraphBuilder(properties(true));
        var prompt = new CharacterPortraitPrompt(
                "zorra naranja con cola esponjosa",
                "small orange fox with a fluffy white-tipped tail",
                List.of("small orange fox", "fluffy white-tipped tail", "bright green eyes"));

        Map<String, Object> graph = builder.build(prompt, 100L);

        String positive = textOf(graph, "601");
        assertTrue(positive.contains("small orange fox with a fluffy white-tipped tail"));
        assertFalse(positive.contains("zorra naranja con cola esponjosa"));
        assertFalse(positive.contains("(small orange fox:1.2)"), "No duplica el sujeto ya presente en la descripción");
        assertFalse(positive.contains("(fluffy white-tipped tail:1.2)"), "No duplica atributos ya presentes en la descripción");
        assertTrue(positive.contains("(bright green eyes:1.2)"));
        assertTrue(positive.contains("transparent background"));
        assertTrue(positive.contains("cute cartoon"));

        assertEquals(100L, seedOf(graph, "701"));
        assertEquals(101L, seedOf(graph, "702"));
        assertEquals("isnet-anime", backgroundModelOf(graph, "851"));

        long removals = graph.values().stream()
                .filter(node -> "Image Remove Background (rembg)".equals(nodeType(node)))
                .count();
        assertEquals(4, removals);
    }

    @Test
    void keepsTheDecodedImageWhenTransparentBackgroundIsDisabled() {
        var builder = new PortraitWorkflowGraphBuilder(properties(false));
        var prompt = new CharacterPortraitPrompt("zorra naranja", "small orange fox", List.of("small orange fox"));

        Map<String, Object> graph = builder.build(prompt, 100L);

        assertNull(graph.get("851"));
        assertEquals(List.of("801", 0), imageRefOf(graph, "901"));
    }

    private ComfyUIProperties properties(boolean transparentBackground) {
        return new ComfyUIProperties(
            "http://localhost:8188", 1, 180, "cartoon.safetensors", "euler", "normal", 20, 7.0,
            "children's book illustration",
            "single character, full body, transparent background",
            "photo, background",
            transparentBackground,
            "isnet-anime",
            "photo",
            "cover",
            512, 512, 1152, 896, 896, 1152,
            List.of(new ComfyUIProperties.LoraSpec("cartoon.safetensors", 0.6, 0.6, "cute cartoon")),
            "ip", "clip", "preset", 3, 0.5
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> inputsOf(Map<String, Object> graph, String nodeId) {
        Map<String, Object> node = (Map<String, Object>) graph.get(nodeId);
        return (Map<String, Object>) node.get("inputs");
    }

    private String textOf(Map<String, Object> graph, String nodeId) {
        return (String) inputsOf(graph, nodeId).get("text");
    }

    private long seedOf(Map<String, Object> graph, String nodeId) {
        return (long) inputsOf(graph, nodeId).get("seed");
    }

    private String backgroundModelOf(Map<String, Object> graph, String nodeId) {
        return (String) inputsOf(graph, nodeId).get("model_name");
    }

    private Object imageRefOf(Map<String, Object> graph, String nodeId) {
        return inputsOf(graph, nodeId).get("images");
    }

    @SuppressWarnings("unchecked")
    private String nodeType(Object node) {
        return (String) ((Map<String, Object>) node).get("class_type");
    }
}
