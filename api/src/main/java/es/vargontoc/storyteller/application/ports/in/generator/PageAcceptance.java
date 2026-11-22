package es.vargontoc.storyteller.application.ports.in.generator;

import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.domain.response.ProposalAccepted;
import es.vargontoc.storyteller.domain.response.ProposalsDiscarded;
import es.vargontoc.storyteller.domain.response.VersionSelected;

/** Elección de versión vigente y aceptación de la portada o de una página pendiente de un cuento con escaleta. */
public interface PageAcceptance {

    /**
     * Marca como vigente una versión del histórico de la portada o de una página pendiente; no llama al modelo.
     * Se rechaza si hay una generación en curso del cuento o si la versión no es de esa página.
     */
    VersionSelected selectVersion(Long storyId, Long pageId, Long versionId);

    /** Promueve la versión vigente a contenido aceptado sin imagen y borra el histórico; no inicia ninguna generación. */
    StoryPage acceptPage(Long storyId, Long pageId);

    /**
     * Descarta todas las propuestas (histórico) de la portada o de una página aceptada reabierta; conserva su contenido
     * aceptado, su imagen y las páginas posteriores. Síncrono y sin turno: se rechaza si hay una generación del cuento.
     */
    ProposalsDiscarded discardProposals(Long storyId, Long pageId);

    /**
     * Acepta la propuesta vigente de la portada o de una página aceptada reabierta. Si hay páginas posteriores (nunca en la
     * portada) y no hay confirmación, no cambia nada y devuelve cuántas se eliminarían; con confirmación sustituye el contenido
     * aceptado (sin imagen), borra su histórico y elimina las posteriores. No inicia ninguna generación.
     */
    ProposalAccepted acceptProposal(Long storyId, Long pageId, boolean confirm);
}
