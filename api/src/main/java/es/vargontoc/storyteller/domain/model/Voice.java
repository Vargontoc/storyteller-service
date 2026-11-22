package es.vargontoc.storyteller.domain.model;

import es.vargontoc.storyteller.shared.BaseModel;

/** Voz subida por el usuario: nombre unico, codigo de idioma y declaracion de permiso sobre el audio. */
public class Voice extends BaseModel {

    private Long id;
    private final String name;
    private final String language;
    private final boolean permissionDeclared;

    public Voice(String name, String language, boolean permissionDeclared) {
        this.name = name;
        this.language = language;
        this.permissionDeclared = permissionDeclared;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public String getLanguage() { return language; }
    public boolean isPermissionDeclared() { return permissionDeclared; }
}
