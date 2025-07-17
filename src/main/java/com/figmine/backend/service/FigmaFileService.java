package com.figmine.backend.service;

import com.figmine.backend.dto.FigmaFileDto;
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
public class FigmaFileService {

    private final WebClient.Builder webClientBuilder;

    @Value("${figma.api.base-url:https://api.figma.com/v1}")
    private String figmaApiBaseUrl;

    // ✅ 1. Get full Figma file
    public FigmaFileDto getFigmaFile(String fileKey, String accessToken) {
        try {
            log.info("Fetching Figma file for fileKey: {}", fileKey);
            WebClient webClient = webClientBuilder.baseUrl(figmaApiBaseUrl).build();
            return webClient.get()
                    .uri("/files/{fileKey}", fileKey)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(FigmaFileDto.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Figma API error (getFigmaFile): {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new FigmaException("API_ERROR", "Error fetching file: " + e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Unexpected error fetching Figma file: {}", e.getMessage(), e);
            throw new FigmaException("INTERNAL_ERROR", "Unexpected error fetching Figma file", e.getMessage());
        }
    }

    // ✅ 2. Get file versions
    public Object getFileVersions(String fileKey, String accessToken) {
        try {
            log.info("Fetching file versions for fileKey: {}", fileKey);
            WebClient webClient = webClientBuilder.baseUrl(figmaApiBaseUrl).build();
            return webClient.get()
                    .uri("/files/{fileKey}/versions", fileKey)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Figma API error (getFileVersions): {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new FigmaException("API_ERROR", "Error fetching file versions: " + e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Unexpected error fetching file versions: {}", e.getMessage(), e);
            throw new FigmaException("INTERNAL_ERROR", "Unexpected error fetching file versions", e.getMessage());
        }
    }

    // ✅ 3. Get specific nodes
    public Object getFileNodes(String fileKey, String nodeIds, String accessToken) {
        try {
            log.info("Fetching nodes {} for fileKey: {}", nodeIds, fileKey);
            WebClient webClient = webClientBuilder.baseUrl(figmaApiBaseUrl).build();
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/files/{fileKey}/nodes")
                            .queryParam("ids", nodeIds)
                            .build(fileKey))
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Figma API error (getFileNodes): {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new FigmaException("API_ERROR", "Error fetching nodes: " + e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Unexpected error fetching nodes: {}", e.getMessage(), e);
            throw new FigmaException("INTERNAL_ERROR", "Unexpected error fetching nodes", e.getMessage());
        }
    }

    // ✅ 4. Get rendered images of frames
    public Object getRenderedImages(String fileKey, String nodeIds, String accessToken) {
        try {
            log.info("Fetching rendered images for nodes {} in fileKey: {}", nodeIds, fileKey);
            WebClient webClient = webClientBuilder.baseUrl(figmaApiBaseUrl).build();
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/images/{fileKey}")
                            .queryParam("ids", nodeIds)
                            .queryParam("format", "png")
                            .build(fileKey))
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Figma API error (getRenderedImages): {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new FigmaException("API_ERROR", "Error fetching images: " + e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Unexpected error fetching rendered images: {}", e.getMessage(), e);
            throw new FigmaException("INTERNAL_ERROR", "Unexpected error fetching rendered images", e.getMessage());
        }
    }
}
