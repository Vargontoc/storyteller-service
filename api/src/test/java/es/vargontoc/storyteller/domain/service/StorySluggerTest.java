package es.vargontoc.storyteller.domain.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StorySluggerTest {

    private final StorySlugger slugger = new StorySlugger();

    @Test
    void simpleTitle() {
        assertEquals("la-luna-dormida", slugger.slugify("La luna dormida"));
    }

    @Test
    void accentsAndEnye() {
        assertEquals("nina-nono-el-pinguino", slugger.slugify("Niña Ñoño, ¡el pingüino!"));
    }

    @Test
    void truncatesWithoutTrailingHyphen() {
        String slug = slugger.slugify("aaaaaaaaaa bbbbbbbbbb cccccccccc dddddddddd eeeee");
        assertEquals(40, slug.length());

        // el corte cae justo tras un separador: se retira el guion final
        String cutAtHyphen = slugger.slugify("a".repeat(39) + " bbbb");
        assertEquals("a".repeat(39), cutAtHyphen);
    }

    @Test
    void tooShortIsInvalid() {
        assertFalse(slugger.isValid(slugger.slugify("¡¡ !!")));
        assertFalse(slugger.isValid(slugger.slugify("Yo")));
        assertFalse(slugger.isValid(slugger.slugify(null)));
        assertTrue(slugger.isValid(slugger.slugify("Sol")));
    }

    @Test
    void deterministic() {
        assertEquals(slugger.slugify("El bosque de Luna"), slugger.slugify("El bosque de Luna"));
    }
}
