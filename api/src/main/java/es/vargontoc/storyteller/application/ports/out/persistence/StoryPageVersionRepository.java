package es.vargontoc.storyteller.application.ports.out.persistence;

import java.util.List;

import es.vargontoc.storyteller.domain.model.PageVersion;

public interface StoryPageVersionRepository {

    /** Guarda la version al final del historico de la pagina; si es vigente, deja de serlo la anterior. */
    PageVersion add(Long pageId, PageVersion version);

    /** Historico de la pagina, de la mas antigua a la mas reciente. */
    List<PageVersion> findByPage(Long pageId);

    /** Marca como vigente una version de la pagina; el resto deja de serlo. */
    void setCurrent(Long pageId, Long versionId);

    /** Borra todas las versiones de la pagina y solo las suyas. */
    void deleteByPage(Long pageId);
}
