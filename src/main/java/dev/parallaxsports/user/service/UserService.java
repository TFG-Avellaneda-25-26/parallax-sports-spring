package dev.parallaxsports.user.service;

import dev.parallaxsports.audit.annotation.Audited;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    private User getuser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse findCurrentUser(String email) {
        return toCurrentUserResponse(getuser(email));
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
    @Audited(action = "USER_EMAIL_CHANGED", entityType = "user")
    public void updateEmail(String email, String newEmail, HttpServletResponse response) {
        User user = getuser(email);

        user.setEmail(newEmail);
        user.setEmailVerified(false);

        userRepository.save(user);

        String accessToken = jwtTokenProvider.issueAccessToken(user);
        String refreshToken = jwtTokenProvider.issueRefreshToken(user);
        Claims refreshClaims = jwtTokenProvider.parseClaims(refreshToken);

        refreshTokenService.revokeAllByUser(user.getId());
        refreshTokenService.store(user, refreshToken, refreshClaims);

        refreshTokenService.addTokenCookie(response, TokenType.ACCESS_TOKEN, accessToken);
        refreshTokenService.addTokenCookie(response, TokenType.REFRESH_TOKEN, refreshToken);
    }

    @Transactional(readOnly = true)
    public boolean validatePassword(String email, String password) {
        User user = getuser(email);

        return passwordEncoder.matches(password, user.getPasswordHash());
    }

    @Transactional
    @Audited(action = "USER_PASSWORD_CHANGED", entityType = "user", includeArgs = false)
    public void updatePassword(String email, String password) {
        User user = getuser(email);

        user.setPasswordHash(passwordEncoder.encode(password));
        userRepository.save(user);
    }

    @Transactional
    @Audited(action = "USER_DISPLAY_NAME_CHANGED", entityType = "user")
    public void updateDisplayName(String email, String displayName) {
        User user = getuser(email);

        user.setDisplayName(displayName);
        userRepository.save(user);
    }

    @Transactional
    @Audited(action = "USER_IDENTITY_DISCONNECTED", entityType = "user_identity")
    public void disconnectIdentity(String email, Long identityId) {
        User user = getuser(email);

        user.getIdentities().removeIf(i -> i.getId().equals(identityId));
        userRepository.save(user);
    }

    @Transactional
    @Audited(action = "USER_ACCOUNT_DELETED", entityType = "user")
    public void deleteAccount(String email, HttpServletResponse response) {
        User user = getuser(email);

        refreshTokenService.revokeAllByUser(user.getId());
        userRepository.delete(user);
        refreshTokenService.clearAuthCookies(response);
    }
}
