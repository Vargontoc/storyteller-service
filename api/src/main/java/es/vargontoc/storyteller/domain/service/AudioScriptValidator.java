package es.vargontoc.storyteller.domain.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Comprueba, página a página, que los fragmentos de un guion de audio reproducen exactamente
 * la secuencia de palabras del texto aceptado. Solo se normalizan mayúsculas y puntuación:
 * una palabra añadida, quitada, reordenada o sustituida (incluida una onomatopeya) invalida la página.
 *
 * Las páginas se numeran desde 1 en el orden recibido (solo interiores, sin portada).
 */
public class AudioScriptValidator {

    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}]+(?:['’-][\\p{L}\\p{N}]+)*");

    /** {@code failedPage} es el número de la primera página que no conserva las palabras, o null si es válido. */
    public record Result(boolean valid, Integer failedPage) {
        static Result ok() {
            return new Result(true, null);
        }

        static Result failedAt(int page) {
            return new Result(false, page);
        }
    }

    public Result validate(List<String> acceptedTexts, List<List<String>> fragmentsByPage) {
        int expected = acceptedTexts.size();
        int received = fragmentsByPage.size();
        if (received != expected) {
            return Result.failedAt(Math.min(expected, received) + 1);
        }
        for (int i = 0; i < expected; i++) {
            if (!words(acceptedTexts.get(i)).equals(words(String.join(" ", nullSafe(fragmentsByPage.get(i)))))) {
                return Result.failedAt(i + 1);
            }
        }
        return Result.ok();
    }

    private static List<String> nullSafe(List<String> fragments) {
        return fragments == null ? List.of() : fragments;
    }

    private static List<String> words(String text) {
        List<String> words = new ArrayList<>();
        Matcher matcher = WORD.matcher(text == null ? "" : text);
        while (matcher.find()) {
            words.add(matcher.group().toLowerCase(Locale.ROOT));
        }
        return words;
    }
}
