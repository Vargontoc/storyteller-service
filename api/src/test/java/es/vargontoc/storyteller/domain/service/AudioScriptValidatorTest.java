package es.vargontoc.storyteller.domain.service;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioScriptValidatorTest {

    private final AudioScriptValidator validator = new AudioScriptValidator();

    private AudioScriptValidator.Result check(String text, String... fragments) {
        return validator.validate(List.of(text), List.of(List.of(fragments)));
    }

    @Test
    void exactTextIsValid() {
        var result = check("El perro corre por el parque.", "El perro corre", "por el parque.");
        assertTrue(result.valid());
        assertNull(result.failedPage());
    }

    @Test
    void differentPunctuationAndCaseIsValid() {
        assertTrue(check("El perro corre. Salta mucho.", "el PERRO, corre", "¡salta... mucho!").valid());
    }

    @Test
    void onomatopoeiaKeptIsValidAndMissingIsNot() {
        assertTrue(check("El perro dijo: ¡Guau! Y corrió.", "El perro dijo: ¡Guau!", "Y corrió.").valid());
        assertFalse(check("El perro dijo: ¡Guau! Y corrió.", "El perro dijo:", "Y corrió.").valid());
    }

    @Test
    void addedWordIsInvalid() {
        assertFalse(check("El perro corre.", "El perro corre mucho.").valid());
    }

    @Test
    void removedWordIsInvalid() {
        assertFalse(check("El perro corre mucho.", "El perro corre.").valid());
    }

    @Test
    void reorderedWordsAreInvalid() {
        assertFalse(check("El perro corre mucho.", "El perro mucho corre.").valid());
    }

    @Test
    void substitutedWordIsInvalid() {
        assertFalse(check("El perro corre.", "El gato corre.").valid());
    }

    @Test
    void indicatesWhichPageOfEightFails() {
        List<String> texts = new ArrayList<>();
        List<List<String>> script = new ArrayList<>();
        for (int page = 1; page <= 8; page++) {
            texts.add("Texto de la página " + page + ".");
            script.add(List.of("Texto de la página " + page + "."));
        }
        script.set(4, List.of("Texto de la página cinco."));

        var result = validator.validate(texts, script);

        assertFalse(result.valid());
        assertEquals(5, result.failedPage());
    }

    @Test
    void missingPageInScriptIsInvalid() {
        var result = validator.validate(List.of("Uno.", "Dos."), List.of(List.of("Uno.")));
        assertFalse(result.valid());
        assertEquals(2, result.failedPage());
    }
}
