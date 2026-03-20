package dev.parallaxsports.notification.model;

import dev.parallaxsports.user.model.User;
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
import lombok.ToString;
import org.hibernate.annotations.Check;

/**
 * Aggregate root representing one user alert for one event/channel/lead-time tuple.
 *
 * Lifecycle status values:
 * - scheduled: ready for dispatch scheduling.
 * - waiting_artifact: blocked until required media artifact metadata exists.
 * - queued: published to transport and pending notification microservice consumption.
 * - processing: accepted by notification microservice.
 * - sent: terminal success.
 * - failed_retryable: transient failure eligible for retry.
 * - failed_permanent: terminal failure.
 * - cancelled: terminal cancellation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "user_event_alerts",
    uniqueConstraints = {
        @UniqueConstraint(name = "user_event_alerts_idempotency_uniq", columnNames = "idempotency_key")
    }
)
@Check(constraints = "channel in ('telegram', 'discord', 'email')")
@Check(constraints = "status in ('scheduled', 'waiting_artifact', 'queued', 'processing', 'sent', 'failed_retryable', 'failed_permanent', 'cancelled')")
@ToString(exclude = "user")
public class UserEventAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(nullable = false)
    private String channel;

    @Column(name = "lead_time_minutes", nullable = false)
    private Integer leadTimeMinutes;

    @Column(name = "send_at_utc", nullable = false)
    private OffsetDateTime sendAtUtc;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    @Builder.Default
    private Integer attempts = 0;

    @Column(name = "max_attempts", nullable = false)
    @Builder.Default
    private Integer maxAttempts = 6;

    @Column(name = "next_retry_at_utc")
    private OffsetDateTime nextRetryAtUtc;

    @Column(name = "queued_at_utc")
    private OffsetDateTime queuedAtUtc;

    @Column(name = "processing_started_at_utc")
    private OffsetDateTime processingStartedAtUtc;

    @Column(name = "dispatched_at_utc")
    private OffsetDateTime dispatchedAtUtc;

    @Column(name = "sent_at_utc")
    private OffsetDateTime sentAtUtc;

    @Column(name = "stream_name")
    private String streamName;

    @Column(name = "stream_message_id")
    private String streamMessageId;

    @Column(name = "provider_message_id")
    private String providerMessageId;

    @Column(name = "worker_id")
    private String workerId;

    @Column(name = "artifact_required", nullable = false)
    @Builder.Default
    private boolean artifactRequired = false;

    @Column(name = "artifact_id")
    private Long artifactId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artifact_id", insertable = false, updatable = false)
    private AlertArtifact artifact;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "last_error_code")
    private String lastErrorCode;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * Initializes audit timestamps when row is inserted.
     */
    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    /**
     * Refreshes updated-at timestamp on each update.
     */
    @jakarta.persistence.PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
