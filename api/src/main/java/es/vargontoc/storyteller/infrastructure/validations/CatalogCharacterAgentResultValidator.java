package es.vargontoc.storyteller.infrastructure.validations;
import java.util.List; import java.util.Map;
import org.springframework.stereotype.Component;
import es.vargontoc.framework.core.error.FieldError;
import es.vargontoc.framework.core.validation.ValidationErrorCode;
import es.vargontoc.framework.core.validation.ValidationResult;
import es.vargontoc.framework.core.validation.Validator;
import es.vargontoc.storyteller.domain.response.CatalogCharacterAgentResult;
@Component public class CatalogCharacterAgentResultValidator implements Validator<CatalogCharacterAgentResult> {
 public ValidationResult validate(CatalogCharacterAgentResult value){
  if(value==null) return ValidationResult.forNullInput();
  if(!value.accepted()) return value.rejectionReason()==null||value.rejectionReason().isBlank()?new ValidationResult(List.of(new FieldError("rejectionReason",ValidationErrorCode.REQUIRED,Map.of()))):new ValidationResult();
  if(value.name()==null||value.name().isBlank()||value.narrativeDescription()==null||value.narrativeDescription().isBlank()||value.visualDescription()==null||value.visualDescription().isBlank()) return new ValidationResult(List.of(new FieldError("catalogCharacter",ValidationErrorCode.REQUIRED,Map.of())));
  if(value.visualDescriptionEn()==null||value.visualDescriptionEn().isBlank()) return new ValidationResult(List.of(new FieldError("visualDescriptionEn",ValidationErrorCode.REQUIRED,Map.of())));
  if(value.visualAttributes()==null||value.visualAttributes().isEmpty()) return new ValidationResult(List.of(new FieldError("visualAttributes",ValidationErrorCode.REQUIRED,Map.of())));
  return new ValidationResult(); }
}
