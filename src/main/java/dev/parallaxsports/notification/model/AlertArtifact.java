package dev.parallaxsports.notification.model;

import dev.parallaxsports.sport.model.Event;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Stores generated media asset metadata associated with an event.
 *
 * Artifact means the metadata needed to reuse generated media during delivery
 * (for example media type, storage key/provider, and public URL).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "alert_artifacts",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "alert_artifacts_event_id_artifact_type_render_context_hash_key",
            columnNames = {"event_id", "artifact_type", "render_context_hash"}
        )
    }
)
public class AlertArtifact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "artifact_type", nullable = false)
    private String artifactType;

    @Column(name = "storage_provider", nullable = false)
    private String storageProvider;

    @Column(name = "storage_key")
    private String storageKey;

    @Column(name = "asset_url", nullable = false)
    private String assetUrl;

    @Column(name = "render_context_hash", nullable = false)
    private String renderContextHash;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    /**
     * Initializes creation timestamp on insert.
     */
    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
