package es.vargontoc.storyteller.application.utils;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import es.vargontoc.storyteller.domain.model.Actor;

public class ReferenceCharacterSelector {
    
    static List<Path> selectReferenceImages(List<Actor> actors, int max){
        return actors.stream()
            .filter(a -> a.getImage() != null && !a.getImage().isBlank())
            .limit(max)
            .map(a -> Path.of(a.getImage()))
            .collect(Collectors.toList());
    }
}
