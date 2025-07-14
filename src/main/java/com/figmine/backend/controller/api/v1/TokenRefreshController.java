package com.figmine.backend.controller.api.v1;

import com.figmine.backend.dto.TokenRefreshRequest;
import com.figmine.backend.model.FigmaToken;
import com.figmine.backend.service.FigmaTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "Authentication and token management")
@RestController
@RequiredArgsConstructor
public class TokenRefreshController extends BaseV1Controller {
    private final FigmaTokenService figmaTokenService;

    @Operation(
        summary = "Refresh access token",
        description = "Refreshes the access token using a refresh token",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Token refreshed successfully",
                content = @Content(schema = @Schema(implementation = FigmaToken.class))
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid request",
                content = @Content
            ),
            @ApiResponse(
                responseCode = "401",
                description = "Invalid or expired refresh token",
                content = @Content
            )
        }
    )
    @PostMapping("/auth/refresh-token")
    public ResponseEntity<FigmaToken> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        FigmaToken token = figmaTokenService.refreshToken(request);
        return ResponseEntity.ok(token);
    }
}
