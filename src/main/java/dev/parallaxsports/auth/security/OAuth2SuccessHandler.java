package dev.parallaxsports.auth.security;

import dev.parallaxsports.auth.service.JwtTokenProvider;
import dev.parallaxsports.auth.service.RefreshTokenService;
import dev.parallaxsports.user.model.User;
import dev.parallaxsports.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    @Override
    public void onAuthenticationSuccess(@NonNull HttpServletRequest request,
                                        @NonNull HttpServletResponse response,
                                        @NonNull Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        if (email == null) {
            throw new RuntimeException("Email not provided by OAuth2 provider");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found after OAuth2 login: " + email));

        String accessToken = jwtTokenProvider.issueAccessToken(user);
        String refreshToken = jwtTokenProvider.issueRefreshToken(user);
        Claims refreshClaims = jwtTokenProvider.parseClaims(refreshToken);

        refreshTokenService.store(user, refreshToken, refreshClaims, request.getRemoteAddr());
        refreshTokenService.addRefreshTokenCookie(response, refreshToken);

        String targetUrl = "http://localhost:4200/auth/callback?token=" + accessToken;
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
