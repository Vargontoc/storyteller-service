package es.vargontoc.storyteller.domain.request;

/** Subida de una voz a la biblioteca. */
public record UploadVoiceCommand(String name, String language, boolean permissionDeclared, byte[] audio) { }
