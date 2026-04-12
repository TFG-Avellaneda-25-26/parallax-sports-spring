package dev.parallaxsports.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthService extends DefaultOAuth2UserService {

    private final OAuthUserProvisioningService oAuthUserProvisioningService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        try {
            OAuth2User oAuth2User = super.loadUser(userRequest);
            oAuthUserProvisioningService.provisionUser(oAuth2User, userRequest);
            return oAuth2User;
        } catch (Exception ex) {
            log.error("Error in OauthService for provider {}: {}",
                    userRequest.getClientRegistration().getRegistrationId(), ex.getMessage());
            throw new OAuth2AuthenticationException("Error processing identity for user");
        }
    }
}
