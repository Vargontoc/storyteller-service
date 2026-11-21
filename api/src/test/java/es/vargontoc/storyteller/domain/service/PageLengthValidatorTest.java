package es.vargontoc.storyteller.domain.service;

import es.vargontoc.storyteller.domain.enums.PageLengthWarning;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageLengthValidatorTest {

    private final PageLengthValidator validator = new PageLengthValidator();

    /** Construye frases de la forma "p p p." repartiendo las palabras entre ellas. */
    private static String text(int sentences, int words) {
        StringBuilder sb = new StringBuilder();
        int base = words / sentences;
        int extra = words % sentences;
        for (int s = 0; s < sentences; s++) {
            int n = base + (s < extra ? 1 : 0);
            for (int w = 0; w < n; w++) {
                sb.append(w > 0 || s > 0 ? " " : "").append(w == 0 ? "Palabra" : "palabra");
            }
            sb.append('.');
        }
        return sb.toString();
    }

    @Test
    void threeSentencesAnd64Words_noWarning() {
        assertEquals(PageLengthWarning.NONE, validator.validate(text(3, 64)));
    }

    @Test
    void twoSentencesAnd67Words_tooManyWords() {
        assertEquals(PageLengthWarning.TOO_MANY_WORDS, validator.validate(text(2, 67)));
    }

    @Test
    void fiveSentencesAnd40Words_sentenceCount() {
        assertEquals(PageLengthWarning.SENTENCE_COUNT, validator.validate(text(5, 40)));
    }

    @Test
    void oneSentenceOf25Words_noWarning() {
        assertEquals(PageLengthWarning.NONE, validator.validate(text(1, 25)));
    }

    @Test
    void fourSentencesAnd66Words_exactLimit_noWarning() {
        assertEquals(PageLengthWarning.NONE, validator.validate(text(4, 66)));
    }

    @Test
    void fiveSentencesAnd70Words_both() {
        assertEquals(PageLengthWarning.BOTH, validator.validate(text(5, 70)));
    }

    @Test
    void noClosingSignOrEmpty_sentenceCount() {
        assertEquals(PageLengthWarning.SENTENCE_COUNT, validator.validate("El conejo salta sin parar"));
        assertEquals(PageLengthWarning.SENTENCE_COUNT, validator.validate(""));
        assertEquals(PageLengthWarning.SENTENCE_COUNT, validator.validate(null));
    }

    @Test
    void dialogueWithDashes_incisoDoesNotAddSentence() {
        String t = "—¡Hola! —dijo el conejo. Luna sonrió. —¿Jugamos? —preguntó.";
        assertEquals(3, validator.countSentences(t));
    }

    @Test
    void dialogueWithQuotes_incisoDoesNotAddSentence() {
        String t = "\"¡Hola!\" dijo el conejo. Luna sonrió.";
        assertEquals(2, validator.countSentences(t));
    }

    @Test
    void ellipsisAndDoubleSigns_countOnce() {
        assertEquals(1, validator.countSentences("Luna esperó... y esperó..."));
        assertEquals(2, validator.countSentences("¿¡Qué es eso!? Luna miró."));
        assertEquals(2, validator.countSentences("¿Quién? ¡Yo!"));
        assertEquals(2, validator.countSentences("¡Hola!!! ¿Qué tal???"));
    }

    @Test
    void dashesAloneAreNotWords() {
        assertEquals(2, validator.countWords("— Hola — dijo."));
    }
}
