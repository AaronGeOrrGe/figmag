package com.figmine.backend.controller;

import com.figmine.backend.model.User;
import com.figmine.backend.service.FigmaTokenService;
import com.figmine.backend.service.JwtService;
import com.figmine.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.core.ParameterizedTypeReference;

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
@RequestMapping("/figma")
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

    @Value("${figma.api.base-url}")
    private String figmaApiBaseUrl;

    @Operation(summary = "Generate Figma OAuth URL")
    @ApiResponse(responseCode = "200", description = "OAuth URL generated successfully")
    @GetMapping("/connect")
    public ResponseEntity<Map<String, String>> getFigmaAuthUrl(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing Authorization header");
        }

        String jwt = authHeader.substring(7);
        jwtService.extractUsername(jwt); // Validate JWT

        String state = URLEncoder.encode(jwt, StandardCharsets.UTF_8);
        String authUrl = UriComponentsBuilder.fromHttpUrl("https://www.figma.com/oauth")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", "files:read file_content:read projects:read")
                .queryParam("response_type", "code")
                .queryParam("state", state)
                .build()
                .toUriString();

        log.info("Generated Figma OAuth URL: {}", authUrl);
        return ResponseEntity.ok(Map.of("url", authUrl));
    }

    @Operation(summary = "Figma OAuth Callback")
    @ApiResponse(responseCode = "200", description = "OAuth token received successfully")
    @GetMapping("/callback")
    public ResponseEntity<Map<String, Object>> handleFigmaCallback(
            @RequestParam String code,
            @RequestParam(required = false) String state
    ) {
        log.info("Figma callback: code={}, state={}", code, state);

        if (state == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing state");

        String jwt = URLDecoder.decode(state, StandardCharsets.UTF_8);
        String email = jwtService.extractUsername(jwt);
        log.info("Decoded user from JWT: {}", email);

        User user = userService.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Map<String, Object> tokenData = exchangeCodeForToken(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to exchange code for token"));

        String accessToken = (String) tokenData.get("access_token");
        String refreshToken = (String) tokenData.get("refresh_token");
        Object expiresInObj = tokenData.get("expires_in");

        if (accessToken == null || refreshToken == null || expiresInObj == null) {
            log.error("Invalid token response: {}", tokenData);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid token response from Figma");
        }

        long expiresIn = Long.parseLong(expiresInObj.toString());
        Instant expiresAt = Instant.now().plus(expiresIn, ChronoUnit.SECONDS);

        figmaTokenService.saveToken(accessToken, refreshToken, user.getId(), expiresAt);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Figma account linked successfully",
                "expires_in", expiresIn
        ));
    }

    private Optional<Map<String, Object>> exchangeCodeForToken(String code) {
        try {
            String credentials = clientId + ":" + clientSecret;
            String encoded = java.util.Base64.getEncoder()
                    .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("redirect_uri", redirectUri);
            form.add("code", code);
            form.add("grant_type", "authorization_code");


            Map<String, Object> response = webClient.post()
                    .uri(figmaApiBaseUrl + "/oauth/token")
                    .header("Authorization", "Basic " + encoded)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(form)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            log.info("Token exchange response: {}", response);
            return Optional.ofNullable(response);
        } catch (Exception e) {
            log.error("Token exchange failed", e);
            return Optional.empty();
        }
    }
}
