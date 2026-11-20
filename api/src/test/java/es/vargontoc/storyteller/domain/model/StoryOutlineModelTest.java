package es.vargontoc.storyteller.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class StoryOutlineModelTest {

    private StoryBeat validBeat() {
        return new StoryBeat("Luna encuentra una piedra", "planteamiento", List.of("Luna"), "curioso", "claro del bosque");
    }

    @Test
    void beatIsBuiltWithFiveFieldsAndExposesThem() {
        StoryBeat beat = validBeat();

        assertEquals("Luna encuentra una piedra", beat.event());
        assertEquals("planteamiento", beat.narrativeFunction());
        assertEquals(List.of("Luna"), beat.participants());
        assertEquals("curioso", beat.tone());
        assertEquals("claro del bosque", beat.visualNote());
        assertTrue(beat.isComplete());
    }

    @Test
    void beatMissingAnyFieldIsIncomplete() {
        assertFalse(new StoryBeat(null, "f", List.of("Luna"), "t", "v").isComplete());
        assertFalse(new StoryBeat("e", " ", List.of("Luna"), "t", "v").isComplete());
        assertFalse(new StoryBeat("e", "f", List.of(), "t", "v").isComplete());
        assertFalse(new StoryBeat("e", "f", null, "t", "v").isComplete());
        assertFalse(new StoryBeat("e", "f", Arrays.asList("Luna", ""), "t", "v").isComplete());
        assertFalse(new StoryBeat("e", "f", List.of("Luna"), "", "v").isComplete());
        assertFalse(new StoryBeat("e", "f", List.of("Luna"), "t", null).isComplete());
    }

    @Test
    void castMemberHasOnlyNameAndRoleAndNoCatalogId() {
        EpisodicCastMember member = new EpisodicCastMember("Bruno", "amigo del bosque");

        assertEquals("Bruno", member.name());
        assertEquals("amigo del bosque", member.role());
        assertTrue(member.isComplete());
        assertFalse(new EpisodicCastMember("Bruno", null).isComplete());
        // Sin identificador de catalogo: los unicos componentes son nombre y funcion.
        assertEquals(List.of("name", "role"),
                Arrays.stream(EpisodicCastMember.class.getRecordComponents()).map(c -> c.getName()).toList());
    }

    @Test
    void outlineIsCompleteOnlyWithAllBeatsAndCastComplete() {
        var cast = List.of(new EpisodicCastMember("Bruno", "amigo"));

        assertTrue(new StoryOutline(cast, List.of(validBeat())).isComplete());
        assertFalse(new StoryOutline(cast, List.of()).isComplete());
        assertFalse(new StoryOutline(cast, List.of(validBeat(), new StoryBeat("e", "f", List.of("L"), "", "v"))).isComplete());
        assertFalse(new StoryOutline(List.of(new EpisodicCastMember("", "amigo")), List.of(validBeat())).isComplete());
    }

    @Test
    void newStoryExposesContractualScriptVersionAndOldStoryDoesNot() {
        Story fresh = new Story();
        fresh.setScriptVersion(StoryScriptVersion.OUTLINE);
        Story old = new Story();

        assertEquals(Integer.valueOf(1), fresh.getScriptVersion());
        assertTrue(fresh.hasOutline());
        assertNull(old.getScriptVersion());
        assertFalse(old.hasOutline());
    }
}
