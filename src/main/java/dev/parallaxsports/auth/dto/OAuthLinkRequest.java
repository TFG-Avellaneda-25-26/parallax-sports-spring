package dev.parallaxsports.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record OAuthLinkRequest(
    @NotBlank String provider,
    @NotBlank String providerSubject,
    String providerUsername,
    String providerEmail
) {
}
