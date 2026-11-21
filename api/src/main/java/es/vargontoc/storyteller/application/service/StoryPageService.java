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
        return accepted;
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
        try {
            generation.run();
            created = true;
        } catch(RuntimeException e) {
            // El resultado se anuncia como FAILED tras liberar el turno
        } finally {
            gate.release();
        }
        if(created)
            notifier.versionPresented(storyId, pageNumber);
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
        StoryPage page = repository.createPending(storyId, number, (long) number);
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
