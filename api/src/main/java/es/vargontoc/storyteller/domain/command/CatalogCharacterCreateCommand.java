package es.vargontoc.storyteller.domain.command;

public record CatalogCharacterCreateCommand(String hint) {
    public boolean automatic() { return hint == null || hint.isBlank(); }
}
