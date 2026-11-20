package es.vargontoc.storyteller.domain.model;

/**
 * Personaje secundario del reparto ligero de un cuento: solo nombre y funcion narrativa.
 * No tiene identificador de catalogo a proposito: no es un {@link CatalogCharacter}.
 */
public record EpisodicCastMember(String name, String role) {

    public boolean isComplete() {
        return name != null && !name.isBlank() && role != null && !role.isBlank();
    }
}
