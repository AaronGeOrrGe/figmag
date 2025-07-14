package com.figmine.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FigmaTokenRefreshResponse(
    @JsonProperty("access_token") String access_token,
    @JsonProperty("refresh_token") String refresh_token,
    @JsonProperty("expires_in") Long expires_in,
    @JsonProperty("token_type") String token_type,
    @JsonProperty("scope") String scope
) {}
