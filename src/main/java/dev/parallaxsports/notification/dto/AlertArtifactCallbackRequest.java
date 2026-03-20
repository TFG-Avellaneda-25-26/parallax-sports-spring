package dev.parallaxsports.notification.dto;

public record AlertArtifactCallbackRequest(
    String artifactType,
    String storageProvider,
    String storageKey,
    String assetUrl,
    String renderContextHash
) {
}
