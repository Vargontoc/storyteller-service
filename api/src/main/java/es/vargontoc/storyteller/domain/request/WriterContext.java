package es.vargontoc.storyteller.domain.request;

import java.util.List;

import es.vargontoc.storyteller.domain.model.CoverPlan;
import es.vargontoc.storyteller.domain.model.EpisodicCastMember;
import es.vargontoc.storyteller.domain.model.StoryBeat;

/**
 * Contexto que recibe el escritor. {@code protagonist} es la instantánea del protagonista guardada en el cuento.
 * Para la portada no hay beat, reparto ni páginas anteriores: usar {@link #cover}.
 */
public record WriterContext(
        String title,
        String synopsis,
        String protagonist,
        List<EpisodicCastMember> cast,
        StoryBeat beat,
        List<String> previousTexts,
        CoverPlan coverPlan) {

    public WriterContext(String title, String synopsis, String protagonist, List<EpisodicCastMember> cast,
            StoryBeat beat, List<String> previousTexts) {
        this(title, synopsis, protagonist, cast, beat, previousTexts, null);
    }

    public static WriterContext cover(String title, String synopsis, String protagonist) {
        return cover(title, synopsis, protagonist, null);
    }

    /** Portada con su planteamiento propio (lugar y tono); {@code coverPlan} puede ser null en cuentos antiguos. */
    public static WriterContext cover(String title, String synopsis, String protagonist, CoverPlan coverPlan) {
        return new WriterContext(title, synopsis, protagonist, List.of(), null, List.of(), coverPlan);
    }

    public boolean isCover() {
        return beat == null;
    }
}
