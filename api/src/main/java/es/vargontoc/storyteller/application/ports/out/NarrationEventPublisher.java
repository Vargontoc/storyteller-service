package es.vargontoc.storyteller.application.ports.out;

import es.vargontoc.storyteller.domain.events.NarrationEvent;

/** Entrega los eventos de narracion de un cuento a los clientes suscritos. */
public interface NarrationEventPublisher {

    void publish(NarrationEvent event);
}
