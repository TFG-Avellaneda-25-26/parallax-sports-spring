package dev.parallaxsports.user.service;

import dev.parallaxsports.auth.model.TokenType;
import dev.parallaxsports.auth.service.JwtTokenProvider;
import dev.parallaxsports.auth.service.RefreshTokenService;
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

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

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
    public Boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional
    public User updateEmail(String email, String newEmail) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setEmail(newEmail);
        user.setEmailVerified(false);
        return userRepository.save(user);
    }

    public void refreshToken(User user, HttpServletResponse response) {
        String accessToken = jwtTokenProvider.issueAccessToken(user);
        String refreshToken = jwtTokenProvider.issueRefreshToken(user);
        Claims refreshClaims = jwtTokenProvider.parseClaims(refreshToken);

        refreshTokenService.revokeAllByUser(user.getId());
        refreshTokenService.store(user, refreshToken, refreshClaims);

        refreshTokenService.addTokenCookie(response, TokenType.ACCESS_TOKEN, accessToken);
        refreshTokenService.addTokenCookie(response, TokenType.REFRESH_TOKEN, refreshToken);
    }
}
