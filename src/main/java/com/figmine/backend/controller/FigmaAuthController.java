package com.figmine.backend.controller;

import com.figmine.backend.model.User;
import com.figmine.backend.service.FigmaTokenService;
import com.figmine.backend.service.JwtService;
import com.figmine.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Tag(name = "Figma Integration", description = "Handles Figma OAuth authentication")
@RestController
@RequestMapping("/api/figma")
@RequiredArgsConstructor
public class FigmaAuthController {

    private final WebClient webClient;
    private final FigmaTokenService figmaTokenService;
    private final UserService userService;
    private final JwtService jwtService;

    @Value("${figma.client.id}")
    private String clientId;

    @Value("${figma.client.secret}")
    private String clientSecret;

    @Value("${figma.client.redirect-uri}")
    private String redirectUri;

    // ✅ Step 1: Updated connect to use JWT in state
    @Operation(summary = "Generate Figma OAuth URL")
    @ApiResponse(responseCode = "200", description = "OAuth URL generated successfully")
    @GetMapping("/connect")
    public ResponseEntity<Map<String, String>> getFigmaAuthUrl(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing Authorization header");
            }

            String jwt = authHeader.substring(7); // Remove "Bearer "
            jwtService.extractUsername(jwt); // Optional: Validate it

            String state = URLEncoder.encode(jwt, StandardCharsets.UTF_8);

            String authUrl = UriComponentsBuilder.fromHttpUrl("https://www.figma.com/oauth")
                    .queryParam("client_id", clientId)
                    .queryParam("redirect_uri", redirectUri)
                    .queryParam("scope", "file_read")
                    .queryParam("response_type", "code")
                    .queryParam("state", state)
                    .build()
                    .toUriString();

            return ResponseEntity.ok(Map.of("url", authUrl));
        } catch (Exception e) {
            log.error("Failed to generate Figma OAuth URL", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "OAuth init failed");
        }
    }

    // ✅ Step 2: Updated callback to extract JWT from state
    @Operation(summary = "Figma OAuth Callback")
    @ApiResponse(responseCode = "200", description = "OAuth token received successfully")
    @GetMapping("/callback")
    public ResponseEntity<Map<String, Object>> handleFigmaCallback(
            @RequestParam String code,
            @RequestParam(required = false) String state
    ) {
        log.info("Figma callback hit: code={}, state={}", code, state);
        try {
            if (state == null) {
                log.error("Missing state param");
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing state");
            }
            String jwt = URLDecoder.decode(state, StandardCharsets.UTF_8);
            log.info("Decoded JWT: {}", jwt);
            String email = jwtService.extractUsername(jwt);
            log.info("Extracted email: {}", email);
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> {
                        log.error("User not found: {}", email);
                        return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
                    });
            log.info("User found: {}", user.getEmail());

            Map<String, Object> tokenData = exchangeCodeForToken(code)
                    .orElseThrow(() -> {
                        log.error("Failed to exchange code");
                        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to exchange code");
                    });
            log.info("Token data: {}", tokenData);

            String accessToken = (String) tokenData.get("access_token");
            String refreshToken = (String) tokenData.get("refresh_token");
            long expiresIn = Long.parseLong(tokenData.get("expires_in").toString());
            Instant expiresAt = Instant.now().plus(expiresIn, ChronoUnit.SECONDS);

            figmaTokenService.saveToken(accessToken, refreshToken, user.getId(), expiresAt);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Figma account linked",
                    "expires_in", expiresIn
            ));
        } catch (ResponseStatusException e) {
            log.error("OAuth callback ResponseStatusException", e);
            throw e;
        } catch (Exception e) {
            log.error("OAuth callback error", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Callback failed");
        }
    }

    // ✅ Helper to exchange code for token
    private Optional<Map<String, Object>> exchangeCodeForToken(String code) {
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("client_id", clientId);
            form.add("client_secret", clientSecret);
            form.add("redirect_uri", redirectUri);
            form.add("code", code);
            form.add("grant_type", "authorization_code");

            return Optional.ofNullable(
                    webClient.post()
                            .uri("https://www.figma.com/api/oauth/token")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .bodyValue(form)
                            .retrieve()
                            .bodyToMono(Map.class)
                            .block()
            );
        } catch (Exception e) {
            log.error("Token exchange failed", e);
            return Optional.empty();
        }
    }
}
