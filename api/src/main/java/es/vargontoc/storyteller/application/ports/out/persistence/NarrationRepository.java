package es.vargontoc.storyteller.application.ports.out.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import es.vargontoc.storyteller.domain.model.StoryNarration;

/** Persistencia de la narracion de un cuento: estado, copia del nombre de la voz y audio por pagina interior. */
public interface NarrationRepository {

    /** Marca el cuento como narrandose y descarta cualquier audio o voz previos. */
    StoryNarration markInProgress(Long storyId);

    Optional<StoryNarration> find(Long storyId);

    /** Narraciones que figuran en curso (las que un reinicio dejo a medias). */
    List<StoryNarration> findInProgress();

    /**
     * Publica de una sola vez los audios de las paginas interiores (clave: numero de pagina, >= 1) y el nombre
     * de la voz, y marca la narracion como terminada. Si algo falla no queda ningun audio ni voz.
     * Rechaza la portada (pagina 0) con IllegalArgumentException.
     */
    void publish(Long storyId, String voiceName, Map<Integer, byte[]> audiosByPage);

    /** Audio de una pagina interior publicada; vacio para la portada o si no hay audio. */
    Optional<byte[]> findAudio(Long storyId, int page);

    /** Marca la narracion con error y deja el cuento sin audio ni voz. */
    void markError(Long storyId);

    /** Borra la narracion entera: audios, voz y estado. */
    void delete(Long storyId);
}
