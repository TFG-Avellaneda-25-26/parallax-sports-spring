package dev.parallaxsports.follow.model;

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
public class UserFollowNotificationChannelId implements Serializable {

    @Column(name = "follow_id", nullable = false)
    private Long followId;

    @Column(name = "channel", nullable = false)
    private String channel;
}
