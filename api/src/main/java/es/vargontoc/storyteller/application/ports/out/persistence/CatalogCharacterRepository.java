package es.vargontoc.storyteller.application.ports.out.persistence;

import es.vargontoc.storyteller.domain.model.CatalogCharacter;
import java.util.List;

public interface CatalogCharacterRepository {
    CatalogCharacter create(CatalogCharacter character);
    List<CatalogCharacter> findAll();
    CatalogCharacter findById(Long id);
    CatalogCharacter rename(Long id, String name);
}
