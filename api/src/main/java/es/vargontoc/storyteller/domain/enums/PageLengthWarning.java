package es.vargontoc.storyteller.domain.enums;

/**
 * Aviso de longitud del texto narrativo de una página.
 */
public enum PageLengthWarning {
    NONE,
    TOO_MANY_WORDS,
    SENTENCE_COUNT,
    BOTH;

    public boolean hasSentenceWarning() {
        return this == SENTENCE_COUNT || this == BOTH;
    }

    public boolean hasWordsWarning() {
        return this == TOO_MANY_WORDS || this == BOTH;
    }
}
