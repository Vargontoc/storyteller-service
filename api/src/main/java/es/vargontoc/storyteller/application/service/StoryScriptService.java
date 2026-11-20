package es.vargontoc.storyteller.application.service;

import java.util.List;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;

import es.vargontoc.storyteller.application.ports.in.generator.StoryScriptReading;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryOutlineRepository;
import es.vargontoc.storyteller.application.ports.out.persistence.StoryRepository;
import es.vargontoc.storyteller.domain.model.Story;
import es.vargontoc.storyteller.domain.model.StoryOutline;
import es.vargontoc.storyteller.domain.response.StoryScript;
import es.vargontoc.storyteller.domain.response.StoryScript.ScriptBeat;

/** Devuelve el guion vigente de un cuento. Un cuento antiguo (sin escaleta) devuelve reparto y beats vacios. */
@Service
public class StoryScriptService implements StoryScriptReading {

    private final StoryRepository stories;
    private final StoryOutlineRepository outlines;

    public StoryScriptService(StoryRepository stories, StoryOutlineRepository outlines) {
        this.stories = stories;
        this.outlines = outlines;
    }

    @Override
    public StoryScript getScript(Long storyId) {
        Story story = stories.getStory(storyId);
        StoryOutline outline = story.hasOutline() ? outlines.find(storyId).orElse(null) : null;
        if (outline == null)
            return new StoryScript(story.getId(), story.getTitle(), story.getSynopsis(), story.getStatus(),
                story.getScriptVersion(), List.of(), List.of());

        List<ScriptBeat> beats = IntStream.range(0, outline.beats().size())
            .mapToObj(i -> {
                var b = outline.beats().get(i);
                return new ScriptBeat(i + 1, b.event(), b.narrativeFunction(), b.participants(), b.tone(), b.visualNote());
            }).toList();
        return new StoryScript(story.getId(), story.getTitle(), story.getSynopsis(), story.getStatus(),
            story.getScriptVersion(), outline.cast(), beats);
    }
}
