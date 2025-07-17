package com.figmine.backend.controller.api.v1;

import com.figmine.backend.dto.FigmaProjectListDto;
import com.figmine.backend.dto.FigmaFileListDto;
import com.figmine.backend.model.FigmaToken;
import com.figmine.backend.model.User;
import com.figmine.backend.repository.UserRepository;
import com.figmine.backend.service.FigmaProjectService;
import com.figmine.backend.service.FigmaTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "Figma Projects", description = "Endpoints for browsing Figma teams, projects, and files")
@RestController
@RequestMapping("/v1/figma")
@RequiredArgsConstructor
public class FigmaProjectController {
    private final FigmaProjectService figmaProjectService;
    private final FigmaTokenService figmaTokenService;
    private final UserRepository userRepository;

    @Operation(
            summary = "Get Figma projects in a team",
            description = "Fetches all projects for a given Figma team using the user's Figma access token",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Projects fetched successfully",
                            content = @Content(schema = @Schema(implementation = FigmaProjectListDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized or token expired",
                            content = @Content
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Team not found",
                            content = @Content
                    )
            }
    )
    @GetMapping("/teams/{teamId}/projects")
    public ResponseEntity<FigmaProjectListDto> getProjectsInTeam(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Figma team ID", required = true)
            @NotBlank @PathVariable String teamId) {

        log.info("[getProjectsInTeam] Start request for teamId: {} by user: {}", teamId, userDetails.getUsername());

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.error("[getProjectsInTeam] User not found: {}", userDetails.getUsername());
                    return new RuntimeException("User not found");
                });

        FigmaToken token = figmaTokenService.findByUserId(user.getId())
                .orElseThrow(() -> {
                    log.error("[getProjectsInTeam] Figma token not found for user: {}", user.getEmail());
                    return new RuntimeException("Figma token not found");
                });

        String accessToken = figmaTokenService.refreshTokenIfNeeded(token).orElse(token.getAccessToken());
        log.info("[getProjectsInTeam] Using Figma access token for user {}: {}", user.getEmail(), accessToken);

        teamId = teamId.trim();
        log.info("[getProjectsInTeam] Calling Figma API for teamId: {}", teamId);

        FigmaProjectListDto projects = figmaProjectService.getProjectsInTeam(teamId, accessToken);
        log.info("[getProjectsInTeam] Successfully fetched projects for teamId: {}", teamId);

        return ResponseEntity.ok(projects);
    }

    @Operation(
            summary = "Get Figma files in a project",
            description = "Fetches all files for a given Figma project using the user's Figma access token",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Files fetched successfully",
                            content = @Content(schema = @Schema(implementation = FigmaFileListDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized or token expired",
                            content = @Content
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Project not found",
                            content = @Content
                    )
            }
    )
    @GetMapping("/projects/{projectId}/files")
    public ResponseEntity<FigmaFileListDto> getFilesInProject(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Figma project ID", required = true)
            @NotBlank @PathVariable String projectId) {

        log.info("[getFilesInProject] Start request for projectId: {} by user: {}", projectId, userDetails.getUsername());

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.error("[getFilesInProject] User not found: {}", userDetails.getUsername());
                    return new RuntimeException("User not found");
                });

        FigmaToken token = figmaTokenService.findByUserId(user.getId())
                .orElseThrow(() -> {
                    log.error("[getFilesInProject] Figma token not found for user: {}", user.getEmail());
                    return new RuntimeException("Figma token not found");
                });

        String accessToken = figmaTokenService.refreshTokenIfNeeded(token).orElse(token.getAccessToken());
        log.info("[getFilesInProject] Using Figma access token for user {}: {}", user.getEmail(), accessToken);

        projectId = projectId.trim();
        log.info("[getFilesInProject] Calling Figma API for projectId: {}", projectId);

        FigmaFileListDto files = figmaProjectService.getFilesInProject(projectId, accessToken);
        log.info("[getFilesInProject] Successfully fetched files for projectId: {}", projectId);

        return ResponseEntity.ok(files);
    }
}
