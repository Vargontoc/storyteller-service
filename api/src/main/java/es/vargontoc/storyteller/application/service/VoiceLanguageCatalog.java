package es.vargontoc.storyteller.application.service;

import es.vargontoc.storyteller.domain.model.VoiceLanguage;
import org.springframework.stereotype.Component;

import java.util.List;

/** Catálogo fijo de idiomas de Chatterbox Multilingual V3, con el español por defecto. */
@Component
public class VoiceLanguageCatalog {

    private static final VoiceLanguage SPANISH = new VoiceLanguage("es", "Español");

    private static final List<VoiceLanguage> LANGUAGES = List.of(
            new VoiceLanguage("ar", "Árabe"),
            new VoiceLanguage("da", "Danés"),
            new VoiceLanguage("de", "Alemán"),
            new VoiceLanguage("el", "Griego"),
            new VoiceLanguage("en", "Inglés"),
            SPANISH,
            new VoiceLanguage("fi", "Finés"),
            new VoiceLanguage("fr", "Francés"),
            new VoiceLanguage("he", "Hebreo"),
            new VoiceLanguage("hi", "Hindi"),
            new VoiceLanguage("it", "Italiano"),
            new VoiceLanguage("ja", "Japonés"),
            new VoiceLanguage("ko", "Coreano"),
            new VoiceLanguage("ms", "Malayo"),
            new VoiceLanguage("nl", "Neerlandés"),
            new VoiceLanguage("no", "Noruego"),
            new VoiceLanguage("pl", "Polaco"),
            new VoiceLanguage("pt", "Portugués"),
            new VoiceLanguage("ru", "Ruso"),
            new VoiceLanguage("sv", "Sueco"),
            new VoiceLanguage("sw", "Suajili"),
            new VoiceLanguage("tr", "Turco"),
            new VoiceLanguage("zh", "Chino"));

    public List<VoiceLanguage> all() {
        return LANGUAGES;
    }

    public VoiceLanguage defaultLanguage() {
        return SPANISH;
    }

    /** Un idioma ausente, vacío o fuera del catálogo no es admisible. */
    public boolean isSupported(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        String normalized = code.trim();
        return LANGUAGES.stream().anyMatch(l -> l.code().equalsIgnoreCase(normalized));
    }
}
