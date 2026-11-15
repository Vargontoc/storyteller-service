package es.vargontoc.storyteller.infrastructure.adapters.in.rest.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record CatalogCharacterRenameRequest(@NotBlank @Size(max=100) String name) {}
