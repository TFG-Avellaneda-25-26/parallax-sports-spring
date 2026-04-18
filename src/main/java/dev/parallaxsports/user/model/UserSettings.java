package dev.parallaxsports.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Check;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_settings")
@Check(constraints = "theme in ('light', 'dark', 'system')")
@Check(constraints = "default_view in ('cards', 'table')")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "user")
public class UserSettings {

    @Id
    @Column(name = "user_id")
    @EqualsAndHashCode.Include
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String theme;

    @Column(name = "default_view", nullable = false)
    private String defaultView;

    @Column(nullable = false)
    private String timezone;

    @Column(nullable = false)
    private String locale;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        if (theme == null) {
            theme = "system";
        }
        if (defaultView == null) {
            defaultView = "cards";
        }
        if (timezone == null) {
            timezone = "UTC";
        }
        if (locale == null) {
            locale = "en";
        }
        updatedAt = OffsetDateTime.now();
    }
}
