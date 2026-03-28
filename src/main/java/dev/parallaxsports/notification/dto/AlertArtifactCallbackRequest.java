package dev.parallaxsports.notification.dto;

/**
 * Artifact callback payload sent by the notification microservice after media
 * generation/upload.
 *
 * Artifact means the generated media asset metadata (for example image type,
 * storage key, and public URL) that Spring persists and links to pending alerts.
 */
public record AlertArtifactCallbackRequest(
    String artifactType,
    String storageProvider,
    String storageKey,
    String assetUrl,
    String renderContextHash
) {
}
