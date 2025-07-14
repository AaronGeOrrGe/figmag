package com.figmine.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public record FigmaFileListDto(
    @JsonProperty("files") List<File> files
) {
    public record File(
        @JsonProperty("key") String key,
        @JsonProperty("name") String name,
        @JsonProperty("thumbnail_url") String thumbnailUrl,
        @JsonProperty("last_modified") String lastModified,
        @JsonProperty("editorType") String editorType
    ) {}
}
