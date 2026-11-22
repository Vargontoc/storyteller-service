package es.vargontoc.storyteller.application.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import es.vargontoc.storyteller.application.ports.in.generator.CoverGeneration;
import es.vargontoc.storyteller.application.ports.in.generator.PageAcceptance;
import es.vargontoc.storyteller.application.ports.in.generator.PageGeneration;
import es.vargontoc.storyteller.domain.service.PageLengthValidator;
import es.vargontoc.storyteller.application.ports.in.generator.StoryPageGeneration;
import es.vargontoc.storyteller.application.ports.in.persistence.StoryPageUseCase;
import es.vargontoc.storyteller.application.ports.out.GenerationGatePort;
import es.vargontoc.storyteller.application.ports.out.GenerationGatePort.GenerationType;
import es.vargontoc.storyteller.application.ports.out.external.OllamaPort;
import es.vargontoc.storyteller.application.ports.out.external.WriterAgent;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryOutlineRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryPageRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryPageVersionRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryPageReviewRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryRepository;
import es.vargontoc.storyteller.domain.command.StoryPageGenerateCommand;
import es.vargontoc.storyteller.domain.command.StoryPageReviewCommand;
import es.vargontoc.storyteller.domain.enums.PageLengthWarning;
import es.vargontoc.storyteller.domain.enums.PageStatus;
import es.vargontoc.storyteller.domain.enums.RevisionStatus;
import es.vargontoc.storyteller.domain.model.Actor;
import es.vargontoc.storyteller.domain.model.PageVersion;
import es.vargontoc.storyteller.domain.request.WriterContext;
import es.vargontoc.storyteller.domain.response.GenerationStarted;
import es.vargontoc.storyteller.domain.response.ProposalAccepted;
import es.vargontoc.storyteller.domain.response.ProposalsDiscarded;
import es.vargontoc.storyteller.domain.response.VersionSelected;
import es.vargontoc.storyteller.domain.response.WriterHintResult;
import es.vargontoc.storyteller.domain.response.WriterResult;
import es.vargontoc.storyteller.domain.enums.StoryStatus;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.model.StoryBeat;
import es.vargontoc.storyteller.domain.model.StoryOutline;
import es.vargontoc.storyteller.domain.model.StoryPage;
import es.vargontoc.storyteller.domain.model.StoryPageReview;
import es.vargontoc.storyteller.domain.response.StoryCoverAgentResult;
import es.vargontoc.storyteller.domain.response.StoryCoverReviewAgentResult;
import es.vargontoc.storyteller.domain.response.StoryPageAgentResult;
import es.vargontoc.storyteller.domain.response.StoryPageReviewAgentResult;
import es.vargontoc.storyteller.infrastructure.adapters.in.rest.dto.ConfirmReviewRequestDto;
import es.vargontoc.storyteller.shared.Constants;
import es.vargontoc.storyteller.shared.exceptions.AppException;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class StoryPageService implements StoryPageGeneration, StoryPageUseCase, CoverGeneration, PageGeneration, PageAcceptance {

    @Value("classpath:/prompts/new_cover.st")
    private Resource creatCoverResource;
    @Value("classpath:/prompts/review_cover.st")
    private Resource reviewCoverResource;

    @Value("classpath:/prompts/new_page.st")
    private Resource createPageResource;
    @Value("classpath:/prompts/review_page.st")
    private Resource reviewPageResource;



    /** Generaciones máximas por solicitud antes de presentar las versiones con aviso. */
    private static final int MAX_ATTEMPTS = 3;

    private final ObjectMapper mapper = new ObjectMapper();
    private final PageLengthValidator lengthValidator = new PageLengthValidator();

    private final OllamaPort ollama;
    private final String model;
    private final ChatClient client;

    
    private final StoryPageRepository repository;
    private final StoryPageReviewRepository reviewRepositoy;
    private final StoryRepository storyRepository;

    private final StoryOutlineRepository outlineRepository;
    private final GenerationGatePort gate;

    private final WriterAgent writer;
    private final StoryPageVersionRepository versions;
    private final StoryPageNotifier notifier;
    private final TaskExecutor executor;

    public StoryPageService(OllamaPort ollama,
        @Qualifier(Constants.BeanNames.AGENT_SCRIPTWRITER_MODEL)String model,
        @Qualifier(Constants.BeanNames.AGENT_SCRIPTWRITER) ChatClient client,
        StoryPageRepository repository,
        StoryPageReviewRepository reviewRepository,
        StoryRepository storyRepository,
        StoryOutlineRepository outlineRepository,
        GenerationGatePort gate,
        WriterAgent writer,
        StoryPageVersionRepository versions,
        StoryPageNotifier notifier,
        TaskExecutor executor) {
            this.writer = writer;
            this.versions = versions;
            this.notifier = notifier;
            this.executor = executor;
            this.ollama = ollama;
            this.model = model;
            this.client = client;
            this.repository = repository;
            this.storyRepository = storyRepository;
            this.reviewRepositoy = reviewRepository;
            this.outlineRepository = outlineRepository;
            this.gate = gate;
    }

    @Override
    public StoryPage generate(StoryPageGenerateCommand cmd) {
        Story story = storyRepository.getStory(cmd.idStory());
        // Bifurcación explícita: los cuentos con escaleta y los antiguos siguen flujos distintos
        return story.hasOutline() ? generateWithOutline(story) : generateLegacy(story);
    }

    /** Cuentos con escaleta: exige el guion aprobado y pasa el beat de la página al escritor. */
    private StoryPage generateWithOutline(Story story) {
        requireNotComplete(story);
        if(story.getStatus() != StoryStatus.SCRIPT_ACCEPTED)
            throw new AppException("Debe aprobarse el guion antes de generar la portada o las páginas", HttpStatus.CONFLICT);
        // La portada de un cuento con escaleta es asíncrona y tiene su propia operación
        if(story.getPages().isEmpty())
            throw new AppException("Primero hay que generar la portada con la operación de portada", HttpStatus.CONFLICT);
        checkWriterAvailable();
        return generateNext(story, page -> readBeat(story.getId(), page));
    }

    /** Portada de un cuento con escaleta: valida, pide turno y responde; la escena se genera en segundo plano. */
    @Override
    public GenerationStarted startCover(Long storyId) {
        Story story = storyRepository.getStory(storyId);
        if(!story.hasOutline())
            throw new AppException("Solo los cuentos con escaleta generan la portada con esta operación", HttpStatus.CONFLICT);
        requireNotComplete(story);
        if(story.getStatus() != StoryStatus.SCRIPT_ACCEPTED)
            throw new AppException("Debe aprobarse el guion antes de generar la portada o las páginas", HttpStatus.CONFLICT);
        if(story.getPages().stream().anyMatch(p -> p.getPage() == 0))
            throw new AppException("Ya hay una portada para esta story: " + storyId, HttpStatus.CONFLICT);

        // El contexto se arma ahora, dentro de la transacción de la solicitud; la tarea asíncrona ya no lee el cuento
        WriterContext context = WriterContext.cover(story.getTitle(), story.getSynopsis(), readActors(story.getCharacters()));
        return launch(storyId, 0, () -> createCover(storyId, writer.writeCover(context)));
    }

    /**
     * Página N de un cuento con escaleta: exige la portada y las páginas anteriores aceptadas y que N sea la siguiente,
     * pide turno y responde; el escritor genera la página en segundo plano. Una sola generación por solicitud.
     */
    @Override
    public GenerationStarted startPage(Long storyId, int number) {
        Story story = storyRepository.getStory(storyId);
        if(!story.hasOutline())
            throw new AppException("Solo los cuentos con escaleta generan la página con esta operación", HttpStatus.CONFLICT);
        requireNotComplete(story);
        if(story.getStatus() != StoryStatus.SCRIPT_ACCEPTED)
            throw new AppException("Debe aprobarse el guion antes de generar la portada o las páginas", HttpStatus.CONFLICT);

        boolean coverAccepted = story.getPages().stream()
            .anyMatch(p -> p.getPage() == 0 && p.getPageStatus() == PageStatus.ACCEPTED);
        if(!coverAccepted)
            throw new AppException("Primero hay que aceptar la portada", HttpStatus.CONFLICT);

        List<StoryPage> previous = story.getPages().stream()
            .filter(p -> p.getPage() != 0).sorted(Comparator.comparingInt(StoryPage::getPage)).toList();
        for(StoryPage p : previous)
            if(p.getPageStatus() != PageStatus.ACCEPTED)
                throw new AppException("Primero hay que aceptar la página " + p.getPage(), HttpStatus.CONFLICT);
        if(number != previous.size() + 1)
            throw new AppException("La página " + number + " no es la siguiente: toca la " + (previous.size() + 1), HttpStatus.CONFLICT);

        // Contexto cerrado antes de pedir turno: instantánea del protagonista del cuento, beat y textos aceptados
        StoryOutline outline = requireOutline(storyId);
        if(number > outline.beats().size())
            throw new AppException("La página " + number + " no tiene beat en la escaleta", HttpStatus.CONFLICT);
        WriterContext context = new WriterContext(story.getTitle(), story.getSynopsis(), readActors(story.getCharacters()),
            outline.cast(), outline.beats().get(number - 1), previous.stream().map(StoryPage::getText).toList());

        return launch(storyId, number, () -> createPage(storyId, number, () -> writer.writePage(context)));
    }

    /**
     * Acepta la versión vigente de la portada o de una página pendiente: pasa a contenido aceptado sin imagen
     * y se borra el histórico. Solo la bloquea un aviso de número de frases o una generación del mismo cuento.
     */
    @Override
    public StoryPage acceptPage(Long storyId, Long pageId) {
        StoryPage page = repository.getPage(pageId);
        if(!storyId.equals(page.getStoryId()))
            throw new AppException("La página " + pageId + " no pertenece al cuento " + storyId, HttpStatus.NOT_FOUND);
        if(page.getPageStatus() != PageStatus.PENDING)
            throw new AppException("Solo se puede aceptar la portada o una página pendiente", HttpStatus.CONFLICT);
        if(gate.isGeneratingStory(storyId))
            throw new AppException("Hay una generación en curso", HttpStatus.CONFLICT);

        PageVersion current = versions.findByPage(pageId).stream()
            .filter(PageVersion::isCurrent).findFirst()
            .orElseThrow(() -> new AppException("La página no tiene versión vigente", HttpStatus.CONFLICT));
        if(current.getWarning().hasSentenceWarning())
            throw new AppException("La versión vigente no cumple el número de frases: elige otra versión válida, "
                + "pide un cambio con hint o regenera sin hint", HttpStatus.CONFLICT);

        StoryPage accepted = repository.accept(pageId, current.getText(), current.getScene());
        versions.deleteByPage(pageId);
        // Última página de la escaleta: el cuento queda completo y listo para audio (la portada no cuenta)
        if(page.getPage() > 0 && outlineRepository.find(storyId).map(o -> o.beats().size() == page.getPage()).orElse(false))
            storyRepository.updateStatus(storyId, StoryStatus.PAGES_ACCEPTED);
        return accepted;
    }

    /** Un cuento con todas las páginas aceptadas no admite generar más páginas (ALT-014). */
    private void requireNotComplete(Story story) {
        if(story.getStatus() == StoryStatus.PAGES_ACCEPTED)
            throw new AppException("El cuento ya tiene todas las páginas aceptadas", HttpStatus.CONFLICT);
    }

    /**
     * Elige como vigente una versión del histórico de la portada o de una página pendiente. Es síncrono y no pide
     * turno: solo se rechaza si hay una generación del mismo cuento. La versión debe pertenecer a esa página.
     * En una página aceptada reabierta solo se eligen entre sus propuestas (el contenido aceptado no está en el histórico).
     */
    @Override
    public VersionSelected selectVersion(Long storyId, Long pageId, Long versionId) {
        StoryPage page = repository.getPage(pageId);
        if(!storyId.equals(page.getStoryId()))
            throw new AppException("La página " + pageId + " no pertenece al cuento " + storyId, HttpStatus.NOT_FOUND);
        if(page.getPageStatus() != PageStatus.PENDING && page.getPageStatus() != PageStatus.ACCEPTED)
            throw new AppException("Solo se puede elegir versión en la portada o en una página pendiente o reabierta", HttpStatus.CONFLICT);
        if(gate.isGeneratingStory(storyId))
            throw new AppException("Hay una generación en curso", HttpStatus.CONFLICT);

        // El histórico solo contiene versiones de trabajo: el contenido aceptado de una reapertura no está en él y no se puede elegir
        PageVersion chosen = versions.findByPage(pageId).stream()
            .filter(v -> versionId != null && versionId.equals(v.getId())).findFirst()
            .orElseThrow(() -> new AppException("La versión " + versionId + " no es una versión elegible de la página " + pageId, HttpStatus.NOT_FOUND));
        versions.setCurrent(pageId, chosen.getId());
        return new VersionSelected(storyId, pageId, chosen.getId(), chosen.getWarning());
    }

    /**
     * Descarta las propuestas de una portada o página aceptada reabierta: borra solo el histórico de esa página real.
     * El contenido aceptado, su imagen y las demás páginas no se tocan. Síncrono y sin turno de la puerta.
     */
    @Override
    public ProposalsDiscarded discardProposals(Long storyId, Long pageId) {
        StoryPage page = repository.getPage(pageId);
        if(!storyId.equals(page.getStoryId()))
            throw new AppException("La página " + pageId + " no pertenece al cuento " + storyId, HttpStatus.NOT_FOUND);
        if(page.getPageStatus() != PageStatus.ACCEPTED)
            throw new AppException("Solo se pueden descartar las propuestas de una portada o página aceptada reabierta", HttpStatus.CONFLICT);
        if(gate.isGeneratingStory(storyId))
            throw new AppException("Hay una generación en curso", HttpStatus.CONFLICT);
        if(versions.findByPage(pageId).isEmpty())
            throw new AppException("La página no tiene propuestas que descartar", HttpStatus.CONFLICT);

        versions.deleteByPage(pageId);
        return new ProposalsDiscarded(storyId, pageId, page.getPageStatus());
    }

    /**
     * Acepta la propuesta vigente de la portada o de una página aceptada reabierta K. La cascada (páginas posteriores a K,
     * nunca en la portada) se calcula y todo se comprueba ANTES de mutar. Sin confirmación y con posteriores, solo informa
     * del contador. Con confirmación (o sin posteriores): K pasa a la propuesta sin imagen, se borra su histórico y se eliminan
     * las posteriores, todo en la transacción del servicio. No inicia ninguna generación.
     */
    @Override
    public ProposalAccepted acceptProposal(Long storyId, Long pageId, boolean confirm) {
        StoryPage page = repository.getPage(pageId);
        if(!storyId.equals(page.getStoryId()))
            throw new AppException("La página " + pageId + " no pertenece al cuento " + storyId, HttpStatus.NOT_FOUND);
        if(page.getPageStatus() != PageStatus.ACCEPTED)
            throw new AppException("Solo se puede aceptar una propuesta de la portada o de una página aceptada reabierta", HttpStatus.CONFLICT);
        if(gate.isGeneratingStory(storyId))
            throw new AppException("Hay una generación en curso", HttpStatus.CONFLICT);

        List<PageVersion> proposals = versions.findByPage(pageId);
        if(proposals.isEmpty())
            throw new AppException("La página no tiene propuestas que aceptar", HttpStatus.CONFLICT);
        PageVersion current = proposals.stream().filter(PageVersion::isCurrent).findFirst()
            .orElseThrow(() -> new AppException("La página no tiene versión vigente", HttpStatus.CONFLICT));
        if(current.getWarning().hasSentenceWarning())
            throw new AppException("La propuesta vigente no cumple el número de frases: elige otra propuesta válida, "
                + "pide un cambio con hint o descarta las propuestas", HttpStatus.CONFLICT);

        int number = page.getPage();
        Story story = number == 0 ? null : storyRepository.getStory(storyId);
        int affected = story == null ? 0 : (int) story.getPages().stream()
            .filter(p -> p.getPage() > number).count();
        if(affected > 0 && !confirm)
            return new ProposalAccepted(storyId, pageId, number, page.getPageStatus(), true, affected);

        // accept ya deja la página sin imagen importada y con el contenido de la propuesta
        StoryPage accepted = repository.accept(pageId, current.getText(), current.getScene());
        versions.deleteByPage(pageId);
        if(affected > 0) {
            repository.deleteAfter(storyId, number);
            // Al borrarse páginas el cuento deja de estar completo: vuelve a guion aceptado (se pueden pedir de nuevo)
            if(story.getStatus() == StoryStatus.PAGES_ACCEPTED)
                storyRepository.updateStatus(storyId, StoryStatus.SCRIPT_ACCEPTED);
        }
        return new ProposalAccepted(storyId, pageId, number, accepted.getPageStatus(), false, affected);
    }

    /**
     * Cambio con hint sobre la portada o una página pendiente o aceptada (reapertura): la versión vigente es la base
     * (en la primera reapertura, el contenido aceptado, que no entra en el histórico ni cambia) y el resultado se añade al
     * histórico como vigente (la página pasa por el bucle de longitud; la portada, no). Si el escritor rechaza el hint
     * se anuncia el motivo y no cambia nada.
     */
    @Override
    public GenerationStarted startHint(Long storyId, Long pageId, String hint) {
        if(hint == null || hint.isBlank())
            throw new AppException("El hint es obligatorio", HttpStatus.BAD_REQUEST);
        StoryPage page = repository.getPage(pageId);
        if(!storyId.equals(page.getStoryId()))
            throw new AppException("La página " + pageId + " no pertenece al cuento " + storyId, HttpStatus.NOT_FOUND);
        if(page.getPageStatus() != PageStatus.PENDING && page.getPageStatus() != PageStatus.ACCEPTED)
            throw new AppException("Solo se puede pedir un cambio sobre la portada o una página pendiente o aceptada", HttpStatus.CONFLICT);

        // Base: la vigente del histórico; en una aceptada sin propuestas (primer hint de la reapertura), su contenido aceptado
        List<PageVersion> history = versions.findByPage(page.getId());
        WriterResult baseResult;
        if(page.getPageStatus() == PageStatus.ACCEPTED && history.isEmpty()) {
            baseResult = new WriterResult(page.getText(), page.getComposition());
        } else {
            PageVersion base = history.stream().filter(PageVersion::isCurrent).findFirst()
                .orElseThrow(() -> new AppException("La página no tiene versión vigente", HttpStatus.CONFLICT));
            baseResult = new WriterResult(base.getText(), base.getScene());
        }

        // Contexto y base cerrados antes de pedir turno, igual que en la generación inicial
        Story story = storyRepository.getStory(storyId);
        int number = page.getPage();
        boolean cover = number == 0;
        WriterContext context = cover
            ? WriterContext.cover(story.getTitle(), story.getSynopsis(), readActors(story.getCharacters()))
            : hintPageContext(story, number);
        String cleanHint = hint.strip();

        Supplier<WriterResult> rewrite = () -> {
            WriterHintResult result = writer.rewrite(context, baseResult, cleanHint);
            if(!result.hintAccepted())
                throw new HintRejectedException(result.rejectionReason());
            return new WriterResult(result.text(), result.scene());
        };
        Long realPageId = page.getId();
        return launch(storyId, number, () -> addHintVersions(realPageId, cover, rewrite));
    }

    /**
     * Regeneración sin hint de una página pendiente: solo si su versión vigente tiene aviso de número de frases
     * (la portada nunca lo tiene). Pide la página al escritor desde su beat, aplica el bucle de longitud y añade
     * el resultado al histórico existente como vigente. Se rechaza sin pedir turno ni llamar al escritor si no procede.
     */
    @Override
    public GenerationStarted startRegenerate(Long storyId, Long pageId) {
        StoryPage page = repository.getPage(pageId);
        if(!storyId.equals(page.getStoryId()))
            throw new AppException("La página " + pageId + " no pertenece al cuento " + storyId, HttpStatus.NOT_FOUND);
        if(page.getPageStatus() != PageStatus.PENDING)
            throw new AppException("Solo se puede regenerar una página pendiente", HttpStatus.CONFLICT);

        PageVersion current = versions.findByPage(page.getId()).stream()
            .filter(PageVersion::isCurrent).findFirst()
            .orElseThrow(() -> new AppException("La página no tiene versión vigente", HttpStatus.CONFLICT));
        if(!current.getWarning().hasSentenceWarning())
            throw new AppException("Solo se puede regenerar sin hint si la versión vigente no cumple el número de frases", HttpStatus.CONFLICT);

        // Contexto cerrado antes de pedir turno, igual que en la generación inicial
        int number = page.getPage();
        WriterContext context = hintPageContext(storyRepository.getStory(storyId), number);
        Long realPageId = page.getId();
        return launch(storyId, number, () -> addHintVersions(realPageId, false, () -> writer.writePage(context)));
    }

    private WriterContext hintPageContext(Story story, int number) {
        StoryOutline outline = requireOutline(story.getId());
        if(number > outline.beats().size())
            throw new AppException("La página " + number + " no tiene beat en la escaleta", HttpStatus.CONFLICT);
        List<String> previousTexts = story.getPages().stream()
            .filter(p -> p.getPage() != 0 && p.getPage() < number)
            .sorted(Comparator.comparingInt(StoryPage::getPage)).map(StoryPage::getText).toList();
        return new WriterContext(story.getTitle(), story.getSynopsis(), readActors(story.getCharacters()),
            outline.cast(), outline.beats().get(number - 1), previousTexts);
    }

    /** Añade al histórico de la página real la nueva versión (portada) o las del bucle de longitud (página); la última queda vigente. */
    private void addHintVersions(Long pageId, boolean cover, Supplier<WriterResult> rewrite) {
        List<PageVersion> generated = cover
            ? List.of(new PageVersion(null, rewrite.get().scene(), PageLengthWarning.NONE, true))
            : generateWithinLimit(rewrite);
        generated.forEach(v -> versions.add(pageId, v));
    }

    /** El escritor rechaza el hint con un motivo; no es un fallo de generación. */
    private static class HintRejectedException extends RuntimeException {
        HintRejectedException(String reason) { super(reason); }
    }

    /** Concede el turno, anuncia el inicio y lanza la generación en segundo plano; si no se puede lanzar, libera el turno. */
    private GenerationStarted launch(Long storyId, int pageNumber, Runnable generation) {
        acquireTurn(storyId);
        try {
            notifier.started(storyId, pageNumber);
            executor.execute(() -> run(storyId, pageNumber, generation));
        } catch(RuntimeException e) {
            gate.release();
            throw e;
        }
        return new GenerationStarted(storyId, pageNumber);
    }

    /** Tarea asíncrona: el turno se libera siempre y después se anuncia el resultado. */
    private void run(Long storyId, int pageNumber, Runnable generation) {
        boolean created = false;
        String rejection = null;
        try {
            generation.run();
            created = true;
        } catch(HintRejectedException e) {
            rejection = e.getMessage();
        } catch(RuntimeException e) {
            // El resultado se anuncia como FAILED tras liberar el turno
        } finally {
            gate.release();
        }
        if(created)
            notifier.versionPresented(storyId, pageNumber);
        else if(rejection != null)
            notifier.hintRejected(storyId, pageNumber, rejection);
        else
            notifier.failed(storyId, pageNumber);
    }

    /**
     * Bucle de longitud, reutilizable por cualquier generación de página: hasta MAX_ATTEMPTS llamadas al escritor
     * dentro del mismo turno. Devuelve la primera versión sin aviso o, si ninguna lo cumple, las tres con su aviso
     * y la última como vigente. Solo trabaja en memoria: nada es visible hasta que el llamador lo persiste, y un
     * fallo en cualquier intento aborta la solicitud entera sin dejar versiones.
     */
    private List<PageVersion> generateWithinLimit(Supplier<WriterResult> generation) {
        List<PageVersion> attempts = new ArrayList<>();
        for(int i = 1; i <= MAX_ATTEMPTS; i++) {
            WriterResult result = generation.get();
            PageLengthWarning warning = lengthValidator.validate(result.text());
            boolean valid = warning == PageLengthWarning.NONE;
            boolean last = valid || i == MAX_ATTEMPTS;
            PageVersion version = new PageVersion(result.text(), result.scene(), warning, last);
            if(valid)
                return List.of(version);
            attempts.add(version);
        }
        return attempts;
    }

    /** Página N pendiente, vinculada al beat N, con las versiones del bucle de longitud (la última, vigente). */
    private void createPage(Long storyId, int number, Supplier<WriterResult> generation) {
        List<PageVersion> generated = generateWithinLimit(generation);
        // beat_id referencia la fila real de story_beat (no el número de página); se lee justo antes de persistir
        Long beatId = outlineRepository.findBeatId(storyId, number)
            .orElseThrow(() -> new AppException("La página " + number + " no tiene beat en la escaleta", HttpStatus.CONFLICT));
        StoryPage page = repository.createPending(storyId, number, beatId);
        try {
            generated.forEach(v -> versions.add(page.getId(), v));
        } catch(RuntimeException e) {
            // Sin versión la página no es consultable: se retira para poder volver a pedirla
            repository.delete(page.getId());
            throw e;
        }
    }

    /** Portada pendiente con la escena como primera versión vigente, sin aviso (la portada no pasa por la validación de longitud). */
    private void createCover(Long storyId, WriterResult result) {
        StoryPage cover = repository.createPending(storyId, 0, null);
        try {
            versions.add(cover.getId(), new PageVersion(null, result.scene(), PageLengthWarning.NONE, true));
        } catch(RuntimeException e) {
            // Sin versión la portada no es consultable: se retira para poder volver a pedirla
            repository.deleteByStory(storyId);
            throw e;
        }
    }

    /** Cuentos antiguos: deduce la página siguiente, sin beat, y genera dentro del turno de la puerta. */
    private StoryPage generateLegacy(Story story) {
        checkWriterAvailable();
        acquireTurn(story.getId());
        try {
            return generateNext(story, page -> "Sin beat: sigue la sinopsis y el resumen acumulado.");
        } finally {
            gate.release();
        }
    }

    private void checkWriterAvailable() {
        if(!ollama.isAvailable(model))
            throw new AppException("El agente encargado de esta operación no está disponible", HttpStatus.BAD_REQUEST);
    }

    private void acquireTurn(Long storyId) {
        if(!gate.tryAcquire(GenerationType.WRITER, storyId))
            throw new AppException("Hay una generación en curso", HttpStatus.CONFLICT);
    }

    private StoryPage generateNext(Story story, IntFunction<String> beatOf) {
        if(story.getPages().isEmpty())
            return generateCover(story);

        List<StoryPage> pages = story.getPages().stream().filter(x -> x.getPage() != 0).toList();
        int totalPages = pages.size();
        int currentPage = totalPages + 1;
        int maxPages = story.getSize().getPages();

        if(currentPage > maxPages)
            throw new AppException("Has alcanzado el máximo de páginas", HttpStatus.CONFLICT);

        String beat = beatOf.apply(currentPage);

        StoryPageAgentResult result = client.prompt()
        .user(u -> u.text(createPageResource)
            .param("synopsis", story.getSynopsis())
            .param("actors", readActors(story.getCharacters()))
            .param("beat", beat)
            .param("summary", story.getAgentSummary())
            .param("current", currentPage).param("total", maxPages))
        .call().entity(StoryPageAgentResult.class);

        // Los secundarios son contexto narrativo del beat: no se crean fichas ni entradas de catalogo
        return repository.create(result, story.getId(), currentPage);
    }

    
    @Override
    public StoryPageReview review(StoryPageReviewCommand review) {
        checkWriterAvailable();

        StoryPage current =  getPage(review.pageId());
        Story story = storyRepository.getStory(current.getStoryId());
        // StoryPageReview es solo del flujo antiguo; los cuentos con escaleta usan versiones
        if(story.hasOutline())
            throw new AppException("Los cuentos con escaleta no usan la revisión de páginas heredada", HttpStatus.CONFLICT);

        acquireTurn(story.getId());
        try {
            return reviewLegacy(review, story, current);
        } finally {
            gate.release();
        }
    }

    private StoryPageReview reviewLegacy(StoryPageReviewCommand review, Story story, StoryPage current) {
        reviewRepositoy.changeStatus(review.pageId(), RevisionStatus.DISCARDED);

        if(current.getPage() == 0)
            return reviewCover(review, story, current);

        List<StoryPage> pages = story.getPages().stream().filter(x -> x.getPage() != 0 && x.getPage() < current.getPage()).toList();
        int totalPages = pages.size();
        int currentPage = totalPages + 1;
        int maxPages = story.getSize().getPages();

        StoryPageReviewAgentResult result = client.prompt()
            .user(u -> {
                try {
                    u.text(reviewPageResource)
                        .param("synopsis", story.getSynopsis())
                        .param("actors", readActors(story.getCharacters()))
                        .param("summary", story.getAgentSummary())
                        .param("current", currentPage + 1)
                        .param("total", maxPages)
                        .param("text", current.getText())
                        .param("scene", mapper.writeValueAsString(current.getComposition()))
                        .param("target", review.target().name())
                        .param("hint", review.hint());
                } catch (JsonProcessingException e) {
                    throw new AppException(e.getMessage(), HttpStatus.BAD_REQUEST);
                }
            })
            .call().entity(StoryPageReviewAgentResult.class);

        

        return reviewRepositoy.createReview(result, review.pageId(), review.target(), review.hint());
    }

    @Override
    public StoryPageReview getReview(Long entityId) {
        repository.getPage(entityId);
        return reviewRepositoy.getPendingReview(entityId);
    }

    private StoryPage generateCover(Story story) {
        if(story.getPages().stream().anyMatch(p -> p.getPage() == 0))
            throw new AppException("Ya hay una portada para esta story: " + story.getId(), HttpStatus.BAD_REQUEST);

        StoryCoverAgentResult result = client.prompt()
        .user(u -> u.text(creatCoverResource)
            .param("synopsis", story.getSynopsis())
            .param("actors", readActors(story.getCharacters())))
        .call().entity(StoryCoverAgentResult.class);

        StoryPage cover = new StoryPage();
        cover.setPage(0);
        cover.setComposition(result.scene());

        return repository.create(
            new StoryPageAgentResult(story.getTitle(), result.scene(), "")
            , story.getId(), 0);
    }

    /** Retira todas las páginas del cuento (portada incluida) por una edición de beat y devuelve cuántas se retiraron. */
    public int deleteInvalidatedPages(Long storyId) {
        return repository.deleteByStory(storyId);
    }

    /** Beat aprobado de la página interior N (página 1 = primer beat), con sus cinco campos. */
    private String readBeat(Long storyId, int page) {
        StoryOutline outline = requireOutline(storyId);
        if(page < 1 || page > outline.beats().size())
            throw new AppException("La página " + page + " no tiene beat en la escaleta", HttpStatus.CONFLICT);
        StoryBeat b = outline.beats().get(page - 1);
        return "- Qué ocurre: " + b.event() + "\n"
            + "- Función narrativa: " + b.narrativeFunction() + "\n"
            + "- Quiénes aparecen: " + String.join(", ", b.participants()) + "\n"
            + "- Emoción o tono: " + b.tone() + "\n"
            + "- Nota visual: " + b.visualNote();
    }

    private StoryOutline requireOutline(Long storyId) {
        return outlineRepository.find(storyId)
            .orElseThrow(() -> new AppException("El cuento no tiene escaleta aprobada", HttpStatus.CONFLICT));
    }

    private String readActors(List<Actor> characters) {
        return characters.stream()
            .map(c ->"- " + c.getName() + " ( " + c.getNarrativeDescription() + " / " + c.getVisualDescription() + " )")
            .collect(Collectors.joining(";\n"));
    }


    private StoryPageReview reviewCover(StoryPageReviewCommand cmd, Story story, StoryPage page) {
        
            StoryCoverReviewAgentResult result = client.prompt()
                .user(u -> {
                    try {
                        u.text(reviewCoverResource)
                            .param("synopsis", story.getSynopsis())
                            .param("actors", readActors(story.getCharacters()))
                            .param("scene", mapper.writeValueAsString(page.getComposition()))
                            .param("hint", cmd.hint());
                    } catch (JsonProcessingException e) {
                        throw new AppException(e.getMessage(), HttpStatus.BAD_REQUEST);
                    }
                })
                .call().entity(StoryCoverReviewAgentResult.class);
    
            StoryPageReviewAgentResult r = new StoryPageReviewAgentResult(story.getTitle(), result.scene(), result.hintAccepted(), result.rejectedReason());
    
            return reviewRepositoy.createReview(r, cmd.pageId(), cmd.target(), cmd.hint());
    }


    @Override
    public StoryPage confirmReview(ConfirmReviewRequestDto request) {
        // 1. Obtenemos la pagina
        StoryPage current = getPage(request.entityId());

        // 2. Obtenemos el review
        StoryPageReview review = getReview(request.entityId());
        if(review == null)
            throw new AppException("No hay una review pendiente para la story:" + current.getId(), HttpStatus.NOT_FOUND);

        if(request.status() == RevisionStatus.DISCARDED)
            reviewRepositoy.changeStatus(request.entityId(), request.status());
        else if(request.status() == RevisionStatus.CONFIRMED){
            reviewRepositoy.changeStatus(request.entityId(), request.status());
            return repository.update(current.getId(), review);
        }

        
        return current;
    }

    @Override
    public StoryPage getPage(Long idPage) {
        return repository.getPage(idPage);
    }
    
}
