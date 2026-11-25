package es.vargontoc.storyteller.domain.response;

import es.vargontoc.storyteller.domain.enums.ExportMissing;

/**
 * Una falta de la exportación: la portada (page 0) o página interior en que falta algo y qué falta.
 * {@code page} es {@link #STORY} cuando afecta a todo el cuento y no a una página. {@code code} es el código estable
 * de la regla del contrato en las faltas {@code CONTRACT_RULE} y de la imagen del protagonista; nulo en el resto.
 */
public record ExportFault(int page, ExportMissing missing, String message, String code) {

    /** Valor de {@code page} para una falta que afecta a todo el cuento. */
    public static final int STORY = -1;

    public ExportFault(int page, ExportMissing missing, String message) {
        this(page, missing, message, null);
    }
}
