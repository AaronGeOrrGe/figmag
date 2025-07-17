package com.figmine.backend.controller.api.v1;

import com.figmine.backend.dto.FigmaFileDto;
import com.figmine.backend.model.FigmaToken;
import com.figmine.backend.model.User;
import com.figmine.backend.service.FigmaFileService;
import com.figmine.backend.service.FigmaTokenService;
import com.figmine.backend.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Figma Files", description = "Endpoints for viewing Figma files")
@RestController
@RequestMapping("/v1/figma/files")
@RequiredArgsConstructor
public class FigmaFileController {
    private final FigmaFileService figmaFileService;
    private final FigmaTokenService figmaTokenService;
    private final UserRepository userRepository;

    @Operation(
        summary = "Get Figma file by key",
        description = "Fetches a Figma file using the user's Figma access token",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Figma file fetched successfully",
                content = @Content(schema = @Schema(implementation = FigmaFileDto.class))
            ),
            @ApiResponse(
                responseCode = "401",
                description = "Unauthorized or token expired",
                content = @Content
            ),
            @ApiResponse(
                responseCode = "404",
                description = "File not found",
                content = @Content
            )
        }
    )
    @GetMapping("/{fileKey}")
    public ResponseEntity<FigmaFileDto> getFigmaFile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Figma file key", required = true)
            @NotBlank @PathVariable String fileKey) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        FigmaToken token = figmaTokenService.findByUserId(user.getId()).orElseThrow();
        String accessToken = figmaTokenService.refreshTokenIfNeeded(token).orElse(token.getAccessToken());
        fileKey = fileKey.trim();
        FigmaFileDto fileDto = figmaFileService.getFigmaFile(fileKey, accessToken);
        return ResponseEntity.ok(fileDto);
    }
}
