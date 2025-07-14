package com.figmine.backend.service;

import com.figmine.backend.model.FigmaToken;

import com.figmine.backend.dto.ErrorResponse;
import com.figmine.backend.dto.ProjectDto;
import com.figmine.backend.exception.FigmaException;
import com.figmine.backend.model.Project;
import com.figmine.backend.model.User;
import com.figmine.backend.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final FigmaTokenService tokenService;
    private final WebClient.Builder webClientBuilder;

    @Transactional
    public List<ProjectDto> getProjects(User user) {
        if (user == null) {
            throw new FigmaException("USER_ERROR", "User not found");
        }

        try {
            // First get local projects
            List<ProjectDto> localProjects = projectRepository.findByOwner(user).stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());

            // Then get Figma projects if token exists and is valid
            FigmaToken token = tokenService.getTokenForUser(user.getId());
            if (token != null && !tokenService.isTokenExpired(token)) {
                WebClient webClient = webClientBuilder.build();

                try {
                    Map<String, Object> figmaResponse = webClient.get()
                            .uri("https://api.figma.com/v1/files")
                            .header("X-Figma-Token", token.getAccessToken())
                            .retrieve()
                            .bodyToMono(Map.class)
                            .block();

                    if (figmaResponse == null || !figmaResponse.containsKey("files")) {
                        throw new FigmaException("API_ERROR", "Invalid response from Figma API");
                    }

                    List<Map<String, Object>> figmaProjects = (List<Map<String, Object>>) figmaResponse.get("files");

                    // Convert Figma projects to our model
                    List<Project> convertedProjects = figmaProjects.stream()
                            .map(figmaProject -> {
                                String name = (String) figmaProject.get("name");
                                String description = (String) figmaProject.get("description");
                                String key = (String) figmaProject.get("key");

                                if (name == null || key == null) {
                                    throw new FigmaException("API_ERROR", "Invalid project data from Figma");
                                }

                                return Project.builder()
                                        .name(name)
                                        .description(description)
                                        .fileUrl(key)
                                        .owner(user)
                                        .build();
                            })
                            .collect(Collectors.toList());

                    // Save new Figma projects
                    projectRepository.saveAll(convertedProjects);

                    // Combine local and Figma projects
                    localProjects.addAll(convertedProjects.stream()
                            .map(this::toDto)
                            .collect(Collectors.toList()));
                } catch (Exception e) {
                    throw new FigmaException("API_ERROR", "Failed to fetch projects from Figma", e.getMessage());
                }
            }

            return localProjects;

        } catch (Exception e) {
            throw new FigmaException("INTERNAL_ERROR", "Error fetching projects", e.getMessage());
        }
    }

    @Transactional
    public ProjectDto createProject(User user, ProjectDto dto) {
        if (user == null) {
            throw new FigmaException("USER_ERROR", "User not found");
        }
        if (dto == null || dto.getName() == null || dto.getFileUrl() == null) {
            throw new FigmaException("VALIDATION_ERROR", "Project name and file URL are required");
        }

        try {
            Project project = Project.builder()
                    .name(dto.getName())
                    .description(dto.getDescription())
                    .fileUrl(dto.getFileUrl())
                    .owner(user)
                    .build();

            return toDto(projectRepository.save(project));

        } catch (Exception e) {
            throw new FigmaException("INTERNAL_ERROR", "Error creating project", e.getMessage());
        }
    }

    @Transactional
    public ProjectDto updateProject(User user, Long id, ProjectDto dto) {
        if (user == null) {
            throw new FigmaException("USER_ERROR", "User not found");
        }
        if (dto == null) {
            throw new FigmaException("VALIDATION_ERROR", "Invalid project data");
        }

        try {
            Project project = projectRepository.findById(id)
                    .orElseThrow(() -> new FigmaException("NOT_FOUND", "Project not found"));

            if (!project.getOwner().getId().equals(user.getId())) {
                throw new FigmaException("AUTH_ERROR", "Unauthorized to update this project");
            }

            project.setName(dto.getName());
            project.setDescription(dto.getDescription());
            project.setFileUrl(dto.getFileUrl());

            return toDto(projectRepository.save(project));

        } catch (Exception e) {
            throw new FigmaException("INTERNAL_ERROR", "Error updating project", e.getMessage());
        }
    }

    @Transactional
    public void deleteProject(User user, Long id) {
        if (user == null) {
            throw new FigmaException("USER_ERROR", "User not found");
        }

        try {
            Project project = projectRepository.findById(id)
                    .orElseThrow(() -> new FigmaException("NOT_FOUND", "Project not found"));

            if (!project.getOwner().getId().equals(user.getId())) {
                throw new FigmaException("AUTH_ERROR", "Unauthorized to delete this project");
            }

            projectRepository.delete(project);

        } catch (Exception e) {
            throw new FigmaException("INTERNAL_ERROR", "Error deleting project", e.getMessage());
        }
    }

    private ProjectDto toDto(Project project) {
        if (project == null) {
            throw new FigmaException("INTERNAL_ERROR", "Cannot convert null project to DTO");
        }

        return ProjectDto.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .fileUrl(project.getFileUrl())
                .build();
    }
}
