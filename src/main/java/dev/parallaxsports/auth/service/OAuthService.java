package dev.parallaxsports.auth.service;

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
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final BotPermissionCacheService botPermissionCacheService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        try {
            OAuth2User oAuth2User = super.loadUser(userRequest);

            String registrationId = userRequest.getClientRegistration().getRegistrationId();
            AuthProvider authProvider = AuthProvider.fromId(registrationId);

            String subject = authProvider.getSubject(oAuth2User);
            String username = authProvider.getUsername(oAuth2User);
            String email = oAuth2User.getAttribute("email");

            User user = userIdentityRepository.findByProviderAndProviderSubject(subject, username)
                    .map(UserIdentity::getUser)
                    .orElse(null);

            if (user == null) {
                log.info("New identity {} detected for email: {}", registrationId, email);

                user = userRepository.findByEmail(email)
                        .orElseGet(() -> {
                            log.info("Creating new User for email: {}", email);
                            return User.builder()
                                    .email(email)
                                    .displayName(username)
                                    .emailVerified(true)
                                    .build();
                        });

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
                user.addIdentity(newIdentity);
            }

            user.setLastLoginAt(OffsetDateTime.now());
            userRepository.save(user);

            return oAuth2User;

        } catch (Exception ex) {
            log.error("Error in OauthService for provider {}: {}",
                    userRequest.getClientRegistration().getRegistrationId(), ex.getMessage());
            throw new OAuth2AuthenticationException("Error processing identity for user");
        }
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
    }
}
