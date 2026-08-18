package es.vargontoc.storyteller.domain.model;

public record StorySummary(long id, String cover, String title, String synopsis, int actors, int size, int pages) {}
