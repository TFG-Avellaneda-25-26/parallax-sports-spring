package dev.parallaxsports.user.dto;

public record UserSettingsResponse(Long userId, String theme, String defaultView, String timezone, String locale) {
}
