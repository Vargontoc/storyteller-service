package es.vargontoc.storyteller.application.ports.out.external;

import java.util.Optional;

/** Quita el fondo de un retrato de protagonista sin reescalarlo. */
public interface CharacterBackgroundRemover {

    /**
     * @param portrait bytes del retrato original
     * @return el PNG con fondo transparente, o vacío si no se pudo obtener la imagen
     *         (fallo recuperable, sin reintento)
     */
    Optional<byte[]> removeBackground(byte[] portrait);
}
