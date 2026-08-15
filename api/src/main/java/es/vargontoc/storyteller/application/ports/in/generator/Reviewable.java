package es.vargontoc.storyteller.application.ports.in.generator;
import es.vargontoc.storyteller.infrastructure.adapters.in.rest.dto.ConfirmReviewRequestDto;

/**
 * 
 * Interfaz con los metodos necesarios para la revisión de un elemento
 * @param <E> Elemento que va aplicarse la revisión
 * @param <R> Elemento review con el resultado parcial
 * @param <Cmd> Objeto que trae la informacion necesaria para iniciar la review
 */
public interface Reviewable<E, R, Cmd> {
    
    /**
     * Obtiene la revisión pendiente del elemento
     * @param entityId Elemento que esta en fase revisión
     * @return elemento parcial de revisión
     */
    R getReview(Long entityId);

    /**
     * Aplica una revisión al elemento
     * @param review Objeto con las propiedades necesarias para iniciar la revisión
     * @return Elemento revisado
     */
    R review(Cmd review);

    /**
     * Confirma o descarta la revisión del elemento
     * @param request 
     * @return El elemento original o elmento con la revisión aplicada
     */
    E confirmReview(ConfirmReviewRequestDto request);
}
