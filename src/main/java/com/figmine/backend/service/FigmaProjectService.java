package com.figmine.backend.service;

import com.figmine.backend.dto.FigmaProjectListDto;
import com.figmine.backend.dto.FigmaFileListDto;
import com.figmine.backend.exception.FigmaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class FigmaProjectService {
    private final WebClient.Builder webClientBuilder;

    @Value("${figma.api.base-url:https://api.figma.com/v1}")
    private String figmaApiBaseUrl;

    public FigmaProjectListDto getProjectsInTeam(String teamId, String accessToken) {
        try {
            WebClient webClient = webClientBuilder.baseUrl(figmaApiBaseUrl).build();
            return webClient.get()
                    .uri("/teams/{teamId}/projects", teamId)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(FigmaProjectListDto.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Figma API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new FigmaException("API_ERROR", "Figma API error: " + e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Unexpected error fetching Figma projects: {}", e.getMessage(), e);
            throw new FigmaException("INTERNAL_ERROR", "Unexpected error fetching Figma projects", e.getMessage());
        }
    }

    public FigmaFileListDto getFilesInProject(String projectId, String accessToken) {
        try {
            log.info("Using Figma access token for projectId {}: {}", projectId, accessToken);
            WebClient webClient = webClientBuilder.baseUrl(figmaApiBaseUrl).build();
            return webClient.get()
                    .uri("/projects/{projectId}/files", projectId)
                    .header("X-Figma-Token", accessToken)
                    .retrieve()
                    .bodyToMono(FigmaFileListDto.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Figma API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new FigmaException("API_ERROR", "Figma API error: " + e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Unexpected error fetching Figma files: {}", e.getMessage(), e);
            throw new FigmaException("INTERNAL_ERROR", "Unexpected error fetching Figma files", e.getMessage());
        }
    }
}
