package com.figmine.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectDto {
    private Long id;
    private String name;
    private String description;
    private String fileUrl;
}
