package dev.parallaxsports.auth.service;

import dev.parallaxsports.core.config.properties.JwtProperties;
import java.time.Instant;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    public String issueAccessToken(Long userId) {
        return encode("acc", userId, jwtProperties.getAccessTokenExpirationSeconds());
    }

    public String issueRefreshToken(Long userId) {
        return encode("ref", userId, jwtProperties.getRefreshTokenExpirationSeconds());
    }

    private String encode(String type, Long userId, long ttlSeconds) {
        String raw = type + ":" + userId + ":" + Instant.now().plusSeconds(ttlSeconds) + ":" + jwtProperties.getSecret();
        return Base64.getEncoder().encodeToString(raw.getBytes());
    }
}
