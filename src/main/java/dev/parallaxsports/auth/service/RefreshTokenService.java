package dev.parallaxsports.auth.service;

import dev.parallaxsports.auth.model.RefreshToken;
import dev.parallaxsports.auth.model.TokenType;
import dev.parallaxsports.auth.repository.RefreshTokenRepository;
import dev.parallaxsports.core.config.properties.JwtProperties;
import dev.parallaxsports.user.model.User;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private static final String BLACKLIST_KEY_PREFIX = "jwt:blacklist:";
    public static final String COOKIE_NAME = "refresh_token";

    private final RefreshTokenRepository refreshTokenRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final JwtProperties jwtProperties;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @Transactional
    public void store(User user, String rawToken, Claims claims) {
        String jti = claims.getId();
        OffsetDateTime expiresAt = claims.getExpiration().toInstant().atOffset(ZoneOffset.UTC);

        RefreshToken entity = RefreshToken.builder()
            .tokenId(jti)
            .user(user)
            .tokenHash(sha256hex(rawToken))
            .expiresAt(expiresAt)
            .build();

        refreshTokenRepository.save(entity);
        log.debug("Stored refresh token jti='{}' userId={} expiresAt='{}'", jti, user.getId(), expiresAt);
    }

    @Transactional(readOnly = true)
    public Optional<RefreshToken> validate(String jti, String rawToken) {
        return refreshTokenRepository.findById(jti)
            .filter(rt -> rt.getRevokedAt() == null)
            .filter(rt -> rt.getExpiresAt().isAfter(OffsetDateTime.now()))
            .filter(rt -> sha256hex(rawToken).equals(rt.getTokenHash()));
    }

    @Transactional
    public void rotateToken(String oldJti, User user, String newRawToken, Claims newClaims) {
        refreshTokenRepository.findById(oldJti).ifPresent(rt -> {
            rt.setRevokedAt(OffsetDateTime.now());
            refreshTokenRepository.save(rt);
        });
        store(user, newRawToken, newClaims);
    }

    @Transactional
    public void revokeByJti(String jti) {
        refreshTokenRepository.findById(jti).ifPresent(rt -> {
            rt.setRevokedAt(OffsetDateTime.now());
            refreshTokenRepository.save(rt);
            log.debug("Revoked refresh token jti='{}'", jti);
        });
    }

    @Transactional
    public void revokeAllByUser(Long userId) {
        int count = refreshTokenRepository.revokeAllByUserId(userId, OffsetDateTime.now());
        log.info("Revoked {} refresh token(s) for userId={}", count, userId);
    }

    public void blacklistAccessToken(String jti, long ttlSeconds) {
        if (ttlSeconds > 0) {
            stringRedisTemplate.opsForValue().set(
                BLACKLIST_KEY_PREFIX + jti, "1", ttlSeconds, TimeUnit.SECONDS
            );
            log.info("Blacklisted access token jti='{}' ttl={}s", jti, ttlSeconds);
        }
    }

    public boolean isAccessTokenBlacklisted(String jti) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(BLACKLIST_KEY_PREFIX + jti));
    }

    public void addTokenCookie(HttpServletResponse response, TokenType type, String rawToken) {
        long maxAge = (type == TokenType.ACCESS_TOKEN)
            ? jwtProperties.getAccessTokenExpirationSeconds()
            : jwtProperties.getRefreshTokenExpirationSeconds();
        ResponseCookie cookie = ResponseCookie.from(type.getCookieName(), rawToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(maxAge)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearAuthCookies(HttpServletResponse response) {
        for (String name : List.of(TokenType.ACCESS_TOKEN.getCookieName(), TokenType.REFRESH_TOKEN.getCookieName())) {
            ResponseCookie cookie = ResponseCookie.from(name, "")
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .path("/")
                    .maxAge(0)
                    .sameSite("Lax")
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }
    }

    private static String sha256hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
