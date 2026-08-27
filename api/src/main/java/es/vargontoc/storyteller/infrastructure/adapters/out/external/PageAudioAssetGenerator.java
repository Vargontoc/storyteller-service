package es.vargontoc.storyteller.infrastructure.adapters.out.external;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import es.vargontoc.storyteller.application.ports.out.ResourceStorage;
import es.vargontoc.storyteller.application.ports.out.external.AudioGeneratorPort;
import es.vargontoc.storyteller.application.utils.AudioAssembler;
import es.vargontoc.storyteller.application.utils.SentenceSegmenter;
import es.vargontoc.storyteller.domain.enums.VoiceTonePreset;
import es.vargontoc.storyteller.domain.model.AudioPage;
import es.vargontoc.storyteller.domain.model.AudioToneParams;
import es.vargontoc.storyteller.domain.request.AudioGenerationRequest;
import es.vargontoc.storyteller.infrastructure.persistence.StoryPageJpaRepository;
import jakarta.transaction.Transactional;

@Component
@Transactional
public class PageAudioAssetGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(PageAudioAssetGenerator.class);

    private final AudioGeneratorPort audioPort;
    private final ResourceStorage storage;
    private final StoryPageJpaRepository pagesRepository;

    public PageAudioAssetGenerator(AudioGeneratorPort audioPort, ResourceStorage storage, StoryPageJpaRepository pagesRepository) {
        this.audioPort = audioPort;
        this.storage = storage;
        this.pagesRepository = pagesRepository;
    }

    @Async
    public void generatePageAudios(Long storyId, List<AudioPage> pages) {
        pages.forEach(p -> {
            LOG.info("Generando audio para pagina: {}", p.page());

            List<String> sentences = SentenceSegmenter.split(p.text());
            List<byte[]> clips = new ArrayList<>();

            for(String sentence: sentences)  {
                AudioToneParams params = resolveParams(sentence, p.tone(), p.word());
                byte[] clip = audioPort.generateAudio(AudioGenerationRequest.withParams(sentence, params));
                clips.add(clip);
                clips.add(AudioAssembler.silence(SentenceSegmenter.silenceAfter(sentence)));
            }

            byte[] audio = AudioAssembler.concat(clips);

            String asset = storage.saveAudio(storyId, Long.valueOf(p.page()), audio);
            LOG.info("Asset generado: {}", asset);
            pagesRepository.setAudio(storyId, p.page(), asset);
            LOG.info("Audio generado y guardado para la pagina: {}", p.page());
        });
    }

    private AudioToneParams resolveParams(String sentence, VoiceTonePreset  tone, String emphasysWord){
        AudioToneParams base = tone.toParams();
        if(emphasysWord != null && !emphasysWord.isBlank() && sentence.contains(emphasysWord)){
            return new AudioToneParams(
                Math.min(base.exageration() + .25, 2.0),
                Math.max(base.cfgWeight() - 0.1, 0.0),
                base.temperature());
        }
        return base;
    }
}
