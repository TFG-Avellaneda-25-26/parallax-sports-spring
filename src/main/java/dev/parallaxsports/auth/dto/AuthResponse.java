package dev.parallaxsports.auth.dto;

public record AuthResponse(
    Long userId,
    boolean emailVerified
) {
}
