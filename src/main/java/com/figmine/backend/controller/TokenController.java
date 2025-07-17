package com.figmine.backend.controller;

import com.figmine.backend.security.TokenBlacklistService;
import com.figmine.backend.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/tokens")
@Tag(name = "Token Management", description = "Endpoints for JWT blacklist, revocation, and validation")
public class TokenController {

    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    @PostMapping("/revoke")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Revoke the current token",
            description = "Adds the JWT from the Authorization header to the blacklist.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Void> revokeCurrentToken(HttpServletRequest request) {
        String token = extractTokenFromHeader(request);
        if (token == null) {
            log.warn("Revoke attempt failed: No Bearer token found");
            return ResponseEntity.badRequest().build();
        }

        try {
            long expiration = jwtService.getExpirationFromToken(token);
            tokenBlacklistService.blacklistToken(token, expiration);
            log.info("Token revoked. Expiration: {}", Instant.ofEpochSecond(expiration));
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Token revocation failed", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/revoke/{token}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Revoke a specific token (Admin only)",
            description = "Adds a specific token to the blacklist by path variable.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Void> revokeTokenByAdmin(@PathVariable String token) {
        try {
            long expiration = jwtService.getExpirationFromToken(token);
            tokenBlacklistService.blacklistToken(token, expiration);
            log.info("Admin revoked token. Expiration: {}", Instant.ofEpochSecond(expiration));
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.warn("Admin token revocation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/check")
    @Operation(
            summary = "Check a token's status",
            description = "Verifies if a token is valid and not blacklisted"
    )
    public ResponseEntity<Map<String, Object>> checkTokenStatus(@RequestParam String token) {
        try {
            boolean isBlacklisted = tokenBlacklistService.isBlacklisted(token);
            boolean isValid = !isBlacklisted && jwtService.validateJwtToken(token);
            long expiration = jwtService.getExpirationFromToken(token);
            long expiresIn = Math.max(0, expiration - Instant.now().getEpochSecond());

            return ResponseEntity.ok(Map.of(
                    "valid", isValid,
                    "blacklisted", isBlacklisted,
                    "expiresAt", Instant.ofEpochSecond(expiration).toString(),
                    "expiresInSeconds", expiresIn
            ));
        } catch (Exception e) {
            log.error("Token status check failed: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "valid", false,
                    "blacklisted", true,
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/blacklist/size")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get current blacklist size (Admin only)",
            description = "Returns how many tokens have been blacklisted",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Map<String, Integer>> getBlacklistSize() {
        int size = tokenBlacklistService.getBlacklistSize();
        return ResponseEntity.ok(Map.of("size", size));
    }

    private String extractTokenFromHeader(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
