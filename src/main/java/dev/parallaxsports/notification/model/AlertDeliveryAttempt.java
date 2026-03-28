package dev.parallaxsports.notification.model;

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
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Audit row for one delivery attempt of a user alert.
 *
 * Attempt rows capture transport diagnostics and notification microservice
 * metadata (worker_id = microservice instance id) plus provider metadata used
 * by retries, observability, and post-mortem analysis.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "alert_delivery_attempts")
public class AlertDeliveryAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alert_id", nullable = false)
    private UserEventAlert alert;

    @Column(name = "attempt_no", nullable = false)
    private Integer attemptNo;

    @Column(nullable = false)
    private String channel;

    @Column(name = "worker_id")
    private String workerId;

    @Column(name = "stream_name")
    private String streamName;

    @Column(name = "stream_message_id")
    private String streamMessageId;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Column(nullable = false)
    private String outcome;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "provider_message_id")
    private String providerMessageId;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    /**
     * Initializes attempt start timestamp on insert.
     */
    @PrePersist
    void onCreate() {
        if (startedAt == null) {
            startedAt = OffsetDateTime.now();
        }
    }
}
