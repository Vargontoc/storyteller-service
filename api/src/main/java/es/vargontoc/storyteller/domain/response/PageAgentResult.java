package es.vargontoc.storyteller.domain.response;

public record PageAgentResult(int page, boolean isCover, boolean isLastPage, int restPages, String text, String promptScene) {}
