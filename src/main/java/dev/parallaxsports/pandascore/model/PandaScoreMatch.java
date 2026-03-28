package dev.parallaxsports.pandascore.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "pandascore_matches")
public class PandaScoreMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pandascore_id", unique = true, nullable = false)
    private Long pandascoreId;

    @Column(nullable = false)
    private String name;

    @Column
    private String leagueName;

    @Column
    private String status;

    @Column
    private String slug;

    @Column
    private String videogame;

    @Column(name = "raw_json", columnDefinition = "text")
    private String rawJson;

    @Column(name = "begin_at")
    private OffsetDateTime beginAt;

    @Column(name = "end_at")
    private OffsetDateTime endAt;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false, columnDefinition = "CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, columnDefinition = "CURRENT_TIMESTAMP")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}

