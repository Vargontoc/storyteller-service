package es.vargontoc.storyteller.infrastructure.adapters.out.persistence;
import org.springframework.stereotype.Repository;
import es.vargontoc.framework.persistence.jpa.mapping.EntityMapperGuard;
import es.vargontoc.framework.persistence.jpa.transaction.WriteTransactionGuard;
import es.vargontoc.storyteller.application.ports.out.persistence.CatalogCharacterRepository;
import es.vargontoc.storyteller.domain.model.ActorMetadata;
import es.vargontoc.storyteller.domain.model.CatalogCharacter;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterJpaEntity;
import es.vargontoc.storyteller.infrastructure.persistence.CatalogCharacterJpaRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import es.vargontoc.storyteller.shared.exceptions.AppException;
@Repository public class CatalogCharacterRepositoryAdapter implements CatalogCharacterRepository {
 private final CatalogCharacterJpaRepository repository;
 public CatalogCharacterRepositoryAdapter(CatalogCharacterJpaRepository repository){this.repository=repository;}
 public CatalogCharacter create(CatalogCharacter c){
  WriteTransactionGuard.requireWriteTransaction();
  CatalogCharacterJpaEntity e=new CatalogCharacterJpaEntity(); e.setName(c.name()); e.setNarrativeDescription(c.narrativeDescription()); e.setVisualDescription(c.visualDescription()); e.setMetadata(new ActorMetadata(c.visualDescriptionEn(), c.visualAttributes()));
  e=repository.save(e); final CatalogCharacterJpaEntity stored=e; return EntityMapperGuard.guard(() -> new CatalogCharacter(stored.getId(),stored.getName(),stored.getNarrativeDescription(),stored.getVisualDescription(),stored.getMetadata().translate(),stored.getMetadata().attributes())); }
 public List<CatalogCharacter> findAll(){return repository.findAll().stream().map(this::map).toList();}
 public CatalogCharacter findById(Long id){return map(repository.findById(id).orElseThrow(()->new AppException("Catálogo no encontrado",HttpStatus.NOT_FOUND)));}
 public CatalogCharacter rename(Long id,String name){CatalogCharacterJpaEntity e=repository.findById(id).orElseThrow(()->new AppException("Catálogo no encontrado",HttpStatus.NOT_FOUND));e.setName(name);return map(repository.save(e));}
 private CatalogCharacter map(CatalogCharacterJpaEntity e){return EntityMapperGuard.guard(()->new CatalogCharacter(e.getId(),e.getName(),e.getNarrativeDescription(),e.getVisualDescription(),e.getMetadata().translate(),e.getMetadata().attributes()));}
}
