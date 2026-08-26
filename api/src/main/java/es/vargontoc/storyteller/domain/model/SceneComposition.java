package es.vargontoc.storyteller.domain.model;

import java.util.List;

public record SceneComposition(String scene, List<ActorAction> actors, String background) {}
