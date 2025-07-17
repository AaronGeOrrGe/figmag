package com.figmine.backend.service;

import com.figmine.backend.dto.ProjectDto;
import com.figmine.backend.exception.FigmaException;
import com.figmine.backend.model.FigmaToken;
import com.figmine.backend.model.Project;
import com.figmine.backend.model.User;
import com.figmine.backend.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final FigmaTokenService tokenService;
    private final WebClient.Builder webClientBuilder;

    @Transactional
    public List<ProjectDto> getProjects(User user) {
        if (user == null) throw new FigmaException("USER_ERROR", "User not found");

        List<ProjectDto> localProjects = projectRepository.findByOwner(user).stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        Optional<FigmaToken> optionalToken = tokenService.findByUserId(user.getId());
        if (optionalToken.isEmpty() || tokenService.isTokenExpired(optionalToken.get())) {
            log.warn("No valid Figma token for user {}, returning local projects only.", user.getEmail());
            return localProjects;
        }

        FigmaToken token = optionalToken.get();

        List<ProjectDto> figmaProjects = fetchFigmaProjects(token.getAccessToken(), user);
        localProjects.addAll(figmaProjects);

        return localProjects;
    }

    private List<ProjectDto> fetchFigmaProjects(String accessToken, User user) {
        try {
            WebClient webClient = webClientBuilder.build();

            // ⚠️ Replace with your correct Figma endpoint
            Map<String, Object> response = webClient.get()
                    .uri("https://api.figma.com/v1/files")
                    .header("X-Figma-Token", accessToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            log.debug("Figma API response: {}", response);

            if (response == null || !response.containsKey("files")) {
                throw new FigmaException("API_ERROR", "Invalid response from Figma API");
            }

            List<Map<String, Object>> figmaFiles = (List<Map<String, Object>>) response.get("files");

            // Get local file URLs for deduplication
            Set<String> existingFileUrls = projectRepository.findByOwner(user).stream()
                    .map(Project::getFileUrl)
                    .collect(Collectors.toSet());

            List<Project> newProjects = figmaFiles.stream()
                    .map(file -> {
                        String name = (String) file.get("name");
                        String fileKey = (String) file.get("key");
                        String description = (String) file.getOrDefault("description", "");

                        if (name == null || fileKey == null) return null;
                        if (existingFileUrls.contains(fileKey)) return null;

                        return Project.builder()
                                .name(name)
                                .fileUrl(fileKey)
                                .description(description)
                                .owner(user)
                                .build();
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (!newProjects.isEmpty()) {
                log.info("Saving {} new Figma projects for user {}", newProjects.size(), user.getEmail());
                projectRepository.saveAll(newProjects);
            }

            return newProjects.stream().map(this::toDto).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed fetching Figma projects: {}", e.getMessage());
            throw new FigmaException("API_ERROR", "Failed fetching projects from Figma", e.getMessage());
        }
    }

    @Transactional
    public ProjectDto createProject(User user, ProjectDto dto) {
        if (user == null) throw new FigmaException("USER_ERROR", "User not found");
        if (dto == null || dto.getName() == null || dto.getFileUrl() == null)
            throw new FigmaException("VALIDATION_ERROR", "Project name and file URL required");

        Project project = Project.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .fileUrl(dto.getFileUrl())
                .owner(user)
                .build();

        return toDto(projectRepository.save(project));
    }

    @Transactional
    public ProjectDto updateProject(User user, Long id, ProjectDto dto) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new FigmaException("NOT_FOUND", "Project not found"));

        if (!project.getOwner().getId().equals(user.getId())) {
            throw new FigmaException("AUTH_ERROR", "Unauthorized to update this project");
        }

        project.setName(dto.getName());
        project.setDescription(dto.getDescription());
        project.setFileUrl(dto.getFileUrl());

        return toDto(projectRepository.save(project));
    }

    @Transactional
    public void deleteProject(User user, Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new FigmaException("NOT_FOUND", "Project not found"));

        if (!project.getOwner().getId().equals(user.getId())) {
            throw new FigmaException("AUTH_ERROR", "Unauthorized to delete this project");
        }

        projectRepository.delete(project);
        log.info("Deleted project {} for user {}", id, user.getEmail());
    }

    private ProjectDto toDto(Project project) {
        return ProjectDto.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .fileUrl(project.getFileUrl())
                .build();
    }
}
