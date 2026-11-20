package es.vargontoc.storyteller.application.ports.in.generator;

import es.vargontoc.storyteller.domain.command.BeatEditCommand;
import es.vargontoc.storyteller.domain.response.BeatEditResult;

/** Edición directa de un beat de la escaleta con validación de edad y aviso de invalidación de páginas. */
public interface BeatEditing {

    BeatEditResult edit(BeatEditCommand command);
}
