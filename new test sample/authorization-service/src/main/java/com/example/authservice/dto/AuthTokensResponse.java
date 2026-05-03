package com.example.authservice.dto;

import java.time.OffsetDateTime;

public record AuthTokensResponse(
        String accessToken,
        OffsetDateTime accessTokenExpiresAt,
        String refreshToken,
        OffsetDateTime refreshTokenExpiresAt,
        String tokenType,
        AuthUserResponse user
) {
}
