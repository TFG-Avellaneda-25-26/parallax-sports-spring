package dev.parallaxsports.user.service;

import dev.parallaxsports.core.exception.ResourceNotFoundException;
import dev.parallaxsports.user.dto.CurrentUserResponse;
import dev.parallaxsports.user.dto.CurrentUserResponse.FollowDto;
import dev.parallaxsports.user.dto.CurrentUserResponse.IdentityDto;
import dev.parallaxsports.user.dto.CurrentUserResponse.SettingsDto;
import dev.parallaxsports.user.dto.CurrentUserResponse.SportSettingsDto;
import dev.parallaxsports.user.model.User;
import dev.parallaxsports.user.model.UserSettings;
import dev.parallaxsports.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public CurrentUserResponse findCurrentUser(String email) {
        User user = userRepository.findByEmailWithFullProfile(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toCurrentUserResponse(user);
    }

    private CurrentUserResponse toCurrentUserResponse(User user) {
        UserSettings s = user.getSettings();
        SettingsDto settings = s == null ? null : new SettingsDto(
            s.getTheme(), s.getDefaultView(), s.getTimezone(), s.getLocale(), s.getUpdatedAt()
        );

        List<SportSettingsDto> sportSettings = user.getSportSettings().stream()
            .map(ss -> new SportSettingsDto(
                ss.getId().getSportId(), ss.isFollowAll(), ss.getEventTypeFilter(), ss.isNotifyDefault()
            ))
            .toList();

        List<FollowDto> follows = user.getSportFollows().stream()
            .map(f -> new FollowDto(
                f.getId(), f.getSportId(), f.getFollowType(),
                f.getCompetitionId(), f.getParticipantId(),
                f.getEventTypeFilter(), f.isNotify()  // maps to notifyEnabled in DTO
            ))
            .toList();

        List<IdentityDto> identities = user.getIdentities().stream()
            .map(i -> new IdentityDto(
                i.getId(), i.getProvider(), i.getProviderUsername(), i.getProviderEmail()
            ))
            .toList();

        return new CurrentUserResponse(
            user.getId(), user.getEmail(), user.getDisplayName(),
            user.getRole(), user.isEmailVerified(),
            settings, sportSettings, follows, identities,
            user.getCreatedAt(), user.getLastLoginAt()
        );
    }
}
