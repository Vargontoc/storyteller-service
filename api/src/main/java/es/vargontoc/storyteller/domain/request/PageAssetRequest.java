package es.vargontoc.storyteller.domain.request;

import es.vargontoc.storyteller.domain.enums.ResourceType;

public record PageAssetRequest(int page, ResourceType type, String asset) {}
