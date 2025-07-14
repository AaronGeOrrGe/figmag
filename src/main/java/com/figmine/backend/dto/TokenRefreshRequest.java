package com.figmine.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TokenRefreshRequest(
    @NotBlank(message = "Refresh token is required")
    @Size(min = 32, max = 500, message = "Refresh token must be between 32 and 500 characters")
    @Pattern(regexp = "^[A-Za-z0-9-_.]+$", message = "Invalid refresh token format")
    String refreshToken
) {}
