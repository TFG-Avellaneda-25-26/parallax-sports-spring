package dev.parallaxsports.user.dto;

import dev.parallaxsports.user.model.UserRole;
import java.time.OffsetDateTime;
import java.util.List;

public record CurrentUserResponse(
    Long id,
    String email,
    String displayName,
    UserRole role,
    boolean emailVerified,
    SettingsDto settings,
    List<SportSettingsDto> sportSettings,
    List<FollowDto> follows,
    List<IdentityDto> identities,
    OffsetDateTime createdAt,
    OffsetDateTime lastLoginAt
) {

    public record SettingsDto(
        String theme,
        String defaultView,
        String timezone,
        String locale,
        OffsetDateTime updatedAt
    ) {}

    public record SportSettingsDto(
        Long sportId,
        boolean followAll,
        List<String> eventTypeFilter,
        boolean notifyDefault
    ) {}

    public record FollowDto(
        Long id,
        Long sportId,
        String followType,
        Long competitionId,
        Long participantId,
        List<String> eventTypeFilter,
        boolean notifyEnabled
    ) {}

    public record IdentityDto(
        Long id,
        String provider,
        String providerUsername,
        String providerEmail
    ) {}
}
