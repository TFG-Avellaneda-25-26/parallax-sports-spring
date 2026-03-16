package dev.parallaxsports.follow.model;

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
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_sport_follows")
@Check(constraints = "follow_type in ('competition', 'participant', 'venue')")
@Check(constraints = "((competition_id is not null)::int + (participant_id is not null)::int + (venue_id is not null)::int) = 1")
@ToString(exclude = "user")
public class UserSportFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "sport_id", nullable = false)
    private Long sportId;

    @Column(name = "follow_type", nullable = false)
    private String followType;

    @Column(name = "competition_id")
    private Long competitionId;

    @Column(name = "participant_id")
    private Long participantId;

    @Column(name = "venue_id")
    private Long venueId;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "event_type_filter", columnDefinition = "text[]")
    @Builder.Default
    private List<String> eventTypeFilter = new ArrayList<>();

    @Column(nullable = false)
    private boolean notify;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
