package com.figmine.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public record FigmaProjectListDto(
    @JsonProperty("projects") List<Project> projects
) {
    public record Project(
        @JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("description") String description,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("updated_at") String updatedAt,
        @JsonProperty("thumbnail_url") String thumbnailUrl
    ) {}
}
