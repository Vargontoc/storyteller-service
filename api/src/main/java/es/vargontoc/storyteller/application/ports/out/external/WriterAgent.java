package es.vargontoc.storyteller.application.ports.out.external;

import es.vargontoc.storyteller.domain.request.WriterContext;
import es.vargontoc.storyteller.domain.response.WriterHintResult;
import es.vargontoc.storyteller.domain.response.WriterResult;

/** Puerto del agente escritor: escena de portada, página desde su beat y nueva versión a partir de un hint. */
public interface WriterAgent {

    /** Escena de portada (el texto del resultado es siempre {@code null}). */
    WriterResult writeCover(WriterContext context);

    /** Texto narrativo y escena de la página del beat del contexto. */
    WriterResult writePage(WriterContext context);

    /** Nueva versión sobre {@code base} aplicando el hint, o rechazo con motivo. Sin beat en el contexto es la portada. */
    WriterHintResult rewrite(WriterContext context, WriterResult base, String hint);
}
