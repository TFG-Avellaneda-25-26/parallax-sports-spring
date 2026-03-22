package dev.parallaxsports.user.model;

import dev.parallaxsports.follow.model.UserSportFollow;
import dev.parallaxsports.follow.model.UserSportSettings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import dev.parallaxsports.notification.model.UserEventAlert;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"identities", "settings", "sportSettings", "sportFollows", "eventAlerts"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(unique = true, length = 320)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "display_name")
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<UserIdentity> identities = new LinkedHashSet<>();

    @OneToOne(mappedBy = "user")
    private UserSettings settings;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<UserSportSettings> sportSettings = new LinkedHashSet<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<UserSportFollow> sportFollows = new LinkedHashSet<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<UserEventAlert> eventAlerts = new LinkedHashSet<>();

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (role == null) {
            role = UserRole.USER;
        }
    }

    public void addIdentity(UserIdentity identity) {
        identities.add(identity);
        identity.setUser(this);
    }

    public void setSettings(UserSettings settings) {
        this.settings = settings;
        if (settings != null) {
            settings.setUser(this);
        }
    }
}
