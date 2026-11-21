package es.vargontoc.storyteller.domain.model;

import es.vargontoc.storyteller.domain.enums.PageLengthWarning;
import es.vargontoc.storyteller.shared.BaseModel;

/**
 * Version de la portada o de una pagina: texto narrativo (null en la portada), escena y aviso de longitud.
 * Nunca lleva imagen.
 */
public class PageVersion extends BaseModel {

    private Long id;
    private final String text;
    private final SceneComposition scene;
    private final PageLengthWarning warning;
    private final boolean current;

    public PageVersion(String text, SceneComposition scene, PageLengthWarning warning, boolean current) {
        this.text = text;
        this.scene = scene;
        this.warning = warning;
        this.current = current;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getText() { return text; }
    public SceneComposition getScene() { return scene; }
    public PageLengthWarning getWarning() { return warning; }
    public boolean isCurrent() { return current; }
}
