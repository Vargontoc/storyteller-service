package es.vargontoc.storyteller.application.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoiceLanguageCatalogTest {

    private final VoiceLanguageCatalog catalog = new VoiceLanguageCatalog();

    @Test
    void spanishIsIncludedAndDefault() {
        assertEquals("es", catalog.defaultLanguage().code());
        assertTrue(catalog.all().contains(catalog.defaultLanguage()));
    }

    @Test
    void catalogLanguageIsSupported() {
        assertTrue(catalog.isSupported("en"));
        assertTrue(catalog.isSupported("es"));
    }

    @Test
    void nullBlankOrUnknownLanguageIsNotSupported() {
        assertFalse(catalog.isSupported(null));
        assertFalse(catalog.isSupported(""));
        assertFalse(catalog.isSupported("   "));
        assertFalse(catalog.isSupported("xx"));
    }
}
