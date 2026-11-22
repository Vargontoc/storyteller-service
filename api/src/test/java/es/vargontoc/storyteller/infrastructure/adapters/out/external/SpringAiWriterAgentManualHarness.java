package es.vargontoc.storyteller.infrastructure.adapters.out.external;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import es.vargontoc.storyteller.application.ports.out.external.WriterAgent;
import es.vargontoc.storyteller.domain.model.EpisodicCastMember;
import es.vargontoc.storyteller.domain.model.StoryBeat;
import es.vargontoc.storyteller.domain.request.WriterContext;
import es.vargontoc.storyteller.domain.response.WriterHintResult;
import es.vargontoc.storyteller.domain.response.WriterResult;

/**
 * Arnés de prueba manual del escritor contra el Ollama real (sin endpoint). No forma parte de la suite:
 * solo se ejecuta con {@code -Dwriter.harness=true}. Imprime los resultados por consola; se juzgan a ojo.
 * Necesita Ollama arrancado y la base de datos del backend (como el resto de pruebas de contexto).
 * El aprovisionador del escritor está activo: si el modelo no existe en Ollama, se crea desde el Modelfile.
 */
@EnabledIfSystemProperty(named = "writer.harness", matches = "true")
@SpringBootTest(properties = {
        "app.agents.character-provisioning.enabled=false",
        "app.agents.director-provisioning.enabled=false",
        "app.agents.audio-narrator-provisioning.enabled=false",
        "app.portraits.reconciliation.enabled=false",
        "app.narration.reconciliation.enabled=false"})
class SpringAiWriterAgentManualHarness {

    @Autowired
    private WriterAgent writer;

    @Test
    void pageAndRejectedHint() {
        // Beat que solo cita al protagonista y al conejo blanco, con nota visual
        StoryBeat beat = new StoryBeat("Luna encuentra al conejo blanco atascado en unas zarzas", "Nudo",
                List.of("Luna", "Conejo blanco"), "Curioso y tierno",
                "El conejo lleva un chaleco azul y las zarzas tienen flores rojas");
        WriterContext context = new WriterContext("El bosque de Luna", "Luna ayuda a un amigo que se ha perdido",
                "Luna, zorra naranja con una bufanda verde",
                List.of(new EpisodicCastMember("Conejo blanco", "Amigo que necesita ayuda")),
                beat, List.of("Luna sale de su casa al amanecer."));

        System.out.println("===== CASO 1: página desde el beat =====");
        System.out.println("Beat: " + beat);
        WriterResult page = writer.writePage(context);
        System.out.println("TEXTO : " + page.text());
        System.out.println("ESCENA: " + page.scene());
        System.out.println("Comprobar a ojo: narra el hecho del beat, recoge chaleco azul / flores rojas, "
                + "sin otros personajes con nombre.");

        System.out.println("===== CASO 2: hint 'que aparezca el lobo' (el beat no tiene lobo) =====");
        WriterHintResult version = writer.rewrite(context, page, "que aparezca el lobo");
        System.out.println("ACEPTADO: " + version.hintAccepted());
        System.out.println("MOTIVO  : " + version.rejectionReason());
        System.out.println("TEXTO   : " + version.text());
        System.out.println("ESCENA  : " + version.scene());
        System.out.println("Esperado: hintAccepted=false con motivo.");
    }
}
