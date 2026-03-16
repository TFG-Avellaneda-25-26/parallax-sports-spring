package dev.parallaxsports.user.dto;

public record UpdateUserSettingsRequest(String theme, String defaultView, String timezone, String locale) {
}
