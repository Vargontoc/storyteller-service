package es.vargontoc.storyteller.domain.model;

public record WorkflowProperties(String path, String positive, String negative, Long seed, int[] dimensions, String lora, double strength, double clip) { }
