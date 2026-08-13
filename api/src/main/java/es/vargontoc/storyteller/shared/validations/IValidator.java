package es.vargontoc.storyteller.shared.validations;

@FunctionalInterface
public interface IValidator<T> {
    
    void validate(T target);
}
