package es.vargontoc.storyteller.domain.service;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Deriva un slug determinista desde un texto: minúsculas, tildes, diéresis y ñ
 * transliteradas a ASCII, cada secuencia no alfanumérica convertida en un guion,
 * sin guiones iniciales ni finales y recortado a 40 caracteres sin guion final.
 * Un resultado de menos de 3 caracteres se considera no válido.
 */
public class StorySlugger {

    public static final int MAX_LENGTH = 40;
    public static final int MIN_LENGTH = 3;

    public String slugify(String text) {
        if (text == null) {
            return "";
        }
        String ascii = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        String slug = ascii.replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        if (slug.length() > MAX_LENGTH) {
            slug = slug.substring(0, MAX_LENGTH).replaceAll("-+$", "");
        }
        return slug;
    }

    public boolean isValid(String slug) {
        return slug != null && slug.length() >= MIN_LENGTH;
    }
}
