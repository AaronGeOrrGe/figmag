package com.figmine.backend.service;

import com.figmine.backend.dto.FigmaTokenRefreshResponse;
import com.figmine.backend.dto.TokenRefreshRequest;
import com.figmine.backend.exception.FigmaException;
import com.figmine.backend.model.FigmaToken;
import com.figmine.backend.model.User;
import com.figmine.backend.repository.FigmaTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FigmaTokenService {

    private final FigmaTokenRepository tokenRepository;
    private final Builder webClientBuilder;

    @Value("${figma.client.id}")
    private String clientId;

    @Value("${figma.client.secret}")
    private String clientSecret;

    @Value("${figma.api.base-url}")
    private String figmaApiBaseUrl;
    // 2. Save or replace existing token
    @Transactional
    public FigmaToken saveToken(String accessToken, String refreshToken, Long userId, Instant expiresAt) {
        log.debug("Saving token for user: {}", userId);
        FigmaToken existing = findByUserId(userId).orElse(null);
        User user = new User();
        user.setId(userId);

        if (existing != null) {
            existing.setAccessToken(accessToken);
            existing.setRefreshToken(refreshToken);
            existing.setExpiresAt(expiresAt);
            return tokenRepository.save(existing);
        } else {
            FigmaToken token = FigmaToken.builder()
                    .user(user)
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresAt(expiresAt)
                    .build();
            return tokenRepository.save(token);
        }
    }

    // 3. Delete token by user
    @Transactional
    public void deleteByUserId(Long userId) {
        log.debug("Deleting token for user: {}", userId);
        tokenRepository.deleteByUser_Id(userId);
    }

    // ✅ 4. Check if token is expired
    public boolean isTokenExpired(FigmaToken token) {
        return token == null || token.getExpiresAt() == null || token.getExpiresAt().isBefore(Instant.now());
    }

    // ✅ 5. Finders
    @Transactional(readOnly = true)
    public Optional<FigmaToken> findByUserId(Long userId) {
        return Optional.ofNullable(tokenRepository.findByUser_Id(userId));
    }

    @Transactional(readOnly = true)
    public Optional<FigmaToken> findByAccessToken(String accessToken) {
        return tokenRepository.findByAccessToken(accessToken);
    }

    // ✅ 6. Refresh Token endpoint
    @Transactional
    public FigmaToken refreshToken(TokenRefreshRequest request) {
        FigmaToken token = tokenRepository.findByRefreshToken(request.refreshToken())
                .orElseThrow(() -> new FigmaException("INVALID_REFRESH_TOKEN", "Refresh token not found or invalid"));

        Optional<String> refreshed = refreshTokenIfNeeded(token);
        if (refreshed.isEmpty()) {
            throw new FigmaException("REFRESH_FAILED", "Could not refresh token");
        }

        return tokenRepository.findById(token.getId())
                .orElseThrow(() -> new FigmaException("TOKEN_NOT_FOUND", "Token not found after refresh"));
    }

    // ✅ 7. Refresh Token If Expired
    @Transactional
    public Optional<String> refreshTokenIfNeeded(FigmaToken token) {
        if (token == null || token.getRefreshToken() == null) {
            log.warn("Cannot refresh token: token or refresh token is null");
            return Optional.empty();
        }

        if (!isTokenExpired(token)) {
            log.debug("Token not expired, no refresh needed");
            return Optional.of(token.getAccessToken());
        }

        log.info("Refreshing expired token for user: {}", token.getUser().getId());

        try {
            WebClient client = webClientBuilder.build();

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("client_id", clientId);
            formData.add("client_secret", clientSecret);
            formData.add("grant_type", "refresh_token");
            formData.add("refresh_token", token.getRefreshToken());

            FigmaTokenRefreshResponse response = client.post()
                    .uri(figmaApiBaseUrl + "/oauth/token")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .onStatus(status -> status.value() >= 400, clientResponse ->
                            clientResponse.bodyToMono(String.class).map(errorBody ->
                                    new RuntimeException("Figma token refresh failed: " + errorBody)
                            )
                    )
                    .bodyToMono(FigmaTokenRefreshResponse.class)
                    .block();

            if (response == null || response.access_token() == null) {
                log.error("Figma token refresh failed: response was null or missing access_token");
                return Optional.empty();
            }

            token.setAccessToken(response.access_token());
            token.setRefreshToken(response.refresh_token() != null ? response.refresh_token() : token.getRefreshToken());
            token.setExpiresAt(Instant.now().plusSeconds(response.expires_in() != null ? response.expires_in() : 3600));
            tokenRepository.save(token);

            log.info("Figma token refreshed for user: {}", token.getUser().getId());
            return Optional.of(token.getAccessToken());

        } catch (Exception e) {
            log.error("Error refreshing Figma token: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
}
