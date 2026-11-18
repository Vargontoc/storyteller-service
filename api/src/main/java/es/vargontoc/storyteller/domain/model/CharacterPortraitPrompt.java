package es.vargontoc.storyteller.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * Datos visuales de un personaje listos para construir el prompt de la plantilla de retrato.
 * <p>
 * {@code visualDescriptionEn} es la fuente principal para el generador de imágenes y
 * {@code visualAttributes} son rasgos atómicos que se enfatizan sin duplicar el texto.
 */
public record CharacterPortraitPrompt(
        String visualDescription,
        String visualDescriptionEn,
        List<String> visualAttributes) {

    public CharacterPortraitPrompt {
        visualAttributes = visualAttributes == null
                ? List.of()
                : visualAttributes.stream().filter(Objects::nonNull).toList();
    }
}
