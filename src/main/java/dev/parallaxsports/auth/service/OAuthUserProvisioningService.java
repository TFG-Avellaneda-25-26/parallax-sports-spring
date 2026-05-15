package dev.parallaxsports.auth.service;

import dev.parallaxsports.audit.service.AuditService;
import dev.parallaxsports.auth.model.AuthProvider;
import dev.parallaxsports.bot.service.BotPermissionCacheService;
import dev.parallaxsports.core.exception.ResourceNotFoundException;
import dev.parallaxsports.core.exception.UnauthorizedException;
import dev.parallaxsports.user.model.User;
import dev.parallaxsports.user.model.UserIdentity;
import dev.parallaxsports.user.repository.UserIdentityRepository;
import dev.parallaxsports.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthUserProvisioningService {

    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final BotPermissionCacheService botPermissionCacheService;
    private final AuditService auditService;

    @Transactional
    public void provisionUser(OAuth2User oAuth2User, OAuth2UserRequest userRequest) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        AuthProvider authProvider = AuthProvider.fromId(registrationId);

        String subject = authProvider.getSubject(oAuth2User);
        String username = authProvider.getUsername(oAuth2User);
        String email = oAuth2User.getAttribute("email");

        UserIdentity identity = userIdentityRepository
                .findByProviderAndProviderSubject(registrationId, subject)
                .orElse(null);

        User user;

        if (identity == null) {
            log.info("New identity {} detected for email: {}", registrationId, email);

            boolean wasNewUser = false;
            user = userRepository.findByEmail(email)
                    .orElse(null);
            if (user == null) {
                log.info("Creating new User for email: {}", email);
                user = User.builder()
                        .email(email)
                        .displayName(username)
                        .emailVerified(true)
                        .build();
                wasNewUser = true;
            }

            if (user.getId() == null) {
                user = userRepository.save(user);
            }

            UserIdentity newIdentity = UserIdentity.builder()
                    .user(user)
                    .provider(registrationId)
                    .providerSubject(subject)
                    .providerUsername(username)
                    .providerEmail(email)
                    .build();

            userIdentityRepository.save(newIdentity);

            if (user.getIdentities() != null) {
                user.addIdentity(newIdentity);
            }

            auditService.record(
                wasNewUser ? "OAUTH_USER_PROVISIONED" : "OAUTH_IDENTITY_LINKED",
                user.getId(), "user", user.getId(),
                Map.of("provider", registrationId, "providerSubject", subject, "email", String.valueOf(email))
            );
        } else {
            user = identity.getUser();
            identity.setProviderUsername(username);
            identity.setProviderEmail(email);
        }

        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public void unlinkIdentity(User requestingUser, String provider, String providerSubject) {
        UserIdentity identity = userIdentityRepository
            .findByProviderAndProviderSubject(provider, providerSubject)
            .orElseThrow(() -> new ResourceNotFoundException("Identity not found"));

        if (!identity.getUser().getId().equals(requestingUser.getId())) {
            throw new UnauthorizedException("Cannot unlink identity belonging to another user");
        }

        userIdentityRepository.delete(identity);
        botPermissionCacheService.evict(provider, providerSubject);
        log.info("Unlinked identity provider='{}' subject='{}' for user '{}'",
            provider, providerSubject, requestingUser.getEmail());
        auditService.record("OAUTH_IDENTITY_UNLINKED", requestingUser.getId(), "user_identity", identity.getId(),
            Map.of("provider", provider, "providerSubject", providerSubject));
    }
}
