package es.vargontoc.storyteller.application.utils;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;



public class SentenceSegmenter {
    private static final Pattern SPLIT = Pattern.compile(
        "(?<=[.!?…])\\s+(?=[¿¡\"A-ZÁÉÍÓÚÑ])"
    );

    public static List<String> split(String pageText)
    {
        return Arrays.stream(SPLIT.split(pageText.trim())).map(String::trim).filter(s -> !s.isBlank()).toList();
    }

    public static Duration silenceAfter(String sentence) {
        String trimmed = sentence.trim();
        if(trimmed.endsWith("...") || trimmed.endsWith(". . .")) return Duration.ofMillis(500);
        if(trimmed.endsWith("!") || trimmed.endsWith("?")) return Duration.ofMillis(350);
        if(trimmed.endsWith(",")) return Duration.ofMillis(120);
        return Duration.ofMillis(200);
    }
}
