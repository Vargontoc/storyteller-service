package es.vargontoc.storyteller.infrastructure.persistence;

import es.vargontoc.storyteller.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "voice")
public class VoiceJpaEntity extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "language", nullable = false, length = 10)
    private String language;

    @Column(name = "permission_declared", nullable = false)
    private boolean permissionDeclared;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public boolean isPermissionDeclared() { return permissionDeclared; }
    public void setPermissionDeclared(boolean permissionDeclared) { this.permissionDeclared = permissionDeclared; }
}
