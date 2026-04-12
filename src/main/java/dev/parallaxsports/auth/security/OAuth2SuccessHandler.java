package dev.parallaxsports.auth.security;

import dev.parallaxsports.auth.model.TokenType;
import dev.parallaxsports.auth.service.JwtTokenProvider;
import dev.parallaxsports.auth.service.RefreshTokenService;
import dev.parallaxsports.user.model.User;
import dev.parallaxsports.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(@NonNull HttpServletRequest request,
                                        @NonNull HttpServletResponse response,
                                        @NonNull Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        if (email == null) {
            log.warn("OAuth2 login failed: provider did not supply an email address");
            getRedirectStrategy().sendRedirect(request, response, frontendUrl + "/auth/callback?error=oauth_failed");
            return;
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            log.warn("OAuth2 login failed: no user found for email='{}' — OAuthService should have created one", email);
            getRedirectStrategy().sendRedirect(request, response, frontendUrl + "/auth/callback?error=oauth_failed");
            return;
        }

        String accessToken = jwtTokenProvider.issueAccessToken(user);
        String refreshToken = jwtTokenProvider.issueRefreshToken(user);
        Claims refreshClaims = jwtTokenProvider.parseClaims(refreshToken);

        refreshTokenService.store(user, refreshToken, refreshClaims);

        refreshTokenService.addTokenCookie(response, TokenType.REFRESH_TOKEN, refreshToken);
        refreshTokenService.addTokenCookie(response, TokenType.ACCESS_TOKEN, accessToken);

        getRedirectStrategy().sendRedirect(request, response, frontendUrl + "/auth/callback");
    }
}
