package es.vargontoc.storyteller.domain.model;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VoiceInfo(
    String name,
    String filename,
    @JsonProperty("original_filename") String originalFilename,
    @JsonProperty("file_extension") String fileExtension,
    @JsonProperty("file_size") String fileSize,
    @JsonProperty("file_hash") String fileHash,
    @JsonProperty("upload_date") Instant uploadDate) {
}
