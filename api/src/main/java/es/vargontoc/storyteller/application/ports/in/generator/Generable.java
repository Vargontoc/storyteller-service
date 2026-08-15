package es.vargontoc.storyteller.application.ports.in.generator;

/**
 * 
 * Interfaz con los metodos necesarios para generación de un elemento
 * @param <E> Elemento que se va auto-generar
 * @param <Cmd> Objeto necesario para inicialiar la auto-generación
 */
public interface Generable<E, Cmd> { 
    /**
     * Genera un elemento
     * @param cmd
     * @return Elemento generado
     */
    E generate(Cmd cmd);
}
