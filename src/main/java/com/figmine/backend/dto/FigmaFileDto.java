package com.figmine.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record FigmaFileDto(
    @JsonProperty("name") String name,
    @JsonProperty("lastModified") String lastModified,
    @JsonProperty("thumbnailUrl") String thumbnailUrl,
    @JsonProperty("document") Map<String, Object> document,
    @JsonProperty("components") Map<String, Object> components,
    @JsonProperty("styles") Map<String, Object> styles
) {}
