package es.vargontoc.storyteller.domain.response;

import java.util.List;

/** Resultado de una exportación: el zip, o todas las faltas que lo impiden (sin zip). */
public record ExportResult(byte[] zip, List<ExportFault> faults) {

    public static ExportResult delivered(byte[] zip) {
        return new ExportResult(zip, List.of());
    }

    public static ExportResult rejected(List<ExportFault> faults) {
        return new ExportResult(null, faults);
    }

    public boolean isDelivered() {
        return zip != null;
    }
}
