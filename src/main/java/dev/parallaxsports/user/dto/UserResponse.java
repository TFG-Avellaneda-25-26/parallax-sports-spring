package dev.parallaxsports.user.dto;

import dev.parallaxsports.user.model.UserRole;

public record UserResponse(Long id, String email, String displayName, UserRole role) {
}
