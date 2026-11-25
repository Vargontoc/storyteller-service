package es.vargontoc.storyteller.domain.model;

import java.util.List;

/**
 * Cuento listo para exportar a Nubi Game: título, imagen de la portada y páginas interiores en orden.
 * Las imágenes y el audio son rutas del almacén (la imagen es la original subida, sin extensión).
 */
public record NubiGameStory(String title, String coverImage, List<Page> pages) {

    /** Página interior: número, texto aceptado, ruta de su imagen y ruta de su audio. */
    public record Page(int number, String text, String image, String audio) {
    }
}
