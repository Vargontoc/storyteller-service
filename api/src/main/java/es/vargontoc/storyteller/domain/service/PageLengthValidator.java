package es.vargontoc.storyteller.domain.service;

import es.vargontoc.storyteller.domain.enums.PageLengthWarning;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Valida la longitud del texto narrativo: de 1 a 4 frases y como máximo 66 palabras.
 *
 * Regla de frases: un grupo de signos de cierre (. ! ? …) cuenta como UNA frase,
 * así "..." , "?!" o "¡¿" no suman de más. Los cierres pueden ir seguidos de comillas.
 * Si tras el cierre (saltando espacios, rayas y comillas) sigue una minúscula, la frase
 * continúa y no cuenta: en «—¡Hola! —dijo el conejo.» hay una sola frase.
 * El texto final sin signo de cierre no cuenta como frase.
 */
public class PageLengthValidator {

    public static final int MAX_WORDS = 66;
    public static final int MIN_SENTENCES = 1;
    public static final int MAX_SENTENCES = 4;

    private static final Pattern CLOSING = Pattern.compile("[.!?…]+[\"'»”’)]*");
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}]+(?:['’-][\\p{L}\\p{N}]+)*");

    public PageLengthWarning validate(String text) {
        String value = text == null ? "" : text;
        int sentences = countSentences(value);
        boolean badSentences = sentences < MIN_SENTENCES || sentences > MAX_SENTENCES;
        boolean tooManyWords = countWords(value) > MAX_WORDS;

        if (badSentences && tooManyWords) {
            return PageLengthWarning.BOTH;
        }
        if (badSentences) {
            return PageLengthWarning.SENTENCE_COUNT;
        }
        return tooManyWords ? PageLengthWarning.TOO_MANY_WORDS : PageLengthWarning.NONE;
    }

    int countWords(String text) {
        Matcher matcher = WORD.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    int countSentences(String text) {
        Matcher matcher = CLOSING.matcher(text);
        int count = 0;
        while (matcher.find()) {
            if (!continuesInLowercase(text, matcher.end())) {
                count++;
            }
        }
        return count;
    }

    private boolean continuesInLowercase(String text, int from) {
        for (int i = from; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c) || "—–-\"'«“‘".indexOf(c) >= 0) {
                continue;
            }
            return Character.isLowerCase(c);
        }
        return false;
    }
}
