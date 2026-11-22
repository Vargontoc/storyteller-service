package es.vargontoc.storyteller.domain.response;

import es.vargontoc.storyteller.domain.enums.NarrationDisabledReason;
import es.vargontoc.storyteller.domain.enums.NarrationStatus;

/**
 * Estado de narracion de un cuento, calculado desde el estado actual de sus paginas interiores.
 *
 * @param hasOutline      false en cuentos antiguos, que no ofrecen narracion
 * @param canNarrate      narrar o renarrar esta habilitado
 * @param disabledReason  motivo si no esta habilitado; null si lo esta
 * @param narrating       hay una narracion en curso
 * @param hasAudio        el cuento tiene audio publicado
 * @param voiceName       voz con la que se narro; null si no hay audio
 * @param lastResult      resultado del ultimo intento (en curso, terminada o con error); null si nunca se narro
 */
public record NarrationState(Long storyId, boolean hasOutline, boolean canNarrate,
        NarrationDisabledReason disabledReason, boolean narrating, boolean hasAudio, String voiceName,
        NarrationStatus lastResult) { }
