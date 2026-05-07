package dev.parallaxsports.notification.discord.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
@EqualsAndHashCode
public class UserDiscordSportDeliveryOverrideId implements Serializable {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "sport_id", nullable = false)
    private Long sportId;
}
