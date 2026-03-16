package dev.parallaxsports.auth.dto;

public record AuthResponse(
    Long userId,
    String accessToken,
    String refreshToken
) {
}
