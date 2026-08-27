package es.vargontoc.storyteller.domain.request;

import java.nio.file.Path;

public record SceneRequest(
    String background,

    String actor1Visual,
    String actor1Action,
    Path actor1path,
    
    String actor2Visual,
    String actor2Action,
    Path actor2path,

    int width,
    int height,
    long seed
){
    
}
