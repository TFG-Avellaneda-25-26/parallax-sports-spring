package dev.parallaxsports.auth.service;

import dev.parallaxsports.auth.model.RefreshToken;
import dev.parallaxsports.auth.repository.RefreshTokenRepository;
import dev.parallaxsports.core.config.properties.JwtProperties;
import dev.parallaxsports.user.model.User;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
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

    @Transactional
    public void store(User user, String rawToken, Claims claims, String clientIp) {
        String jti = claims.getId();
        OffsetDateTime expiresAt = claims.getExpiration().toInstant().atOffset(ZoneOffset.UTC);

        RefreshToken entity = RefreshToken.builder()
            .tokenId(jti)
            .user(user)
            .tokenHash(sha256hex(rawToken))
            .ipAddress(clientIp)
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
    public void rotateToken(String oldJti, User user, String newRawToken, Claims newClaims, String clientIp) {
        refreshTokenRepository.findById(oldJti).ifPresent(rt -> {
            rt.setRevokedAt(OffsetDateTime.now());
            refreshTokenRepository.save(rt);
        });
        store(user, newRawToken, newClaims, clientIp);
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


    public void addRefreshTokenCookie(HttpServletResponse response, String rawRefreshToken) {
        Cookie cookie = new Cookie(COOKIE_NAME, rawRefreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // TODO: we change it when de deploy?
        cookie.setPath("/");
        cookie.setMaxAge((int) jwtProperties.getRefreshTokenExpirationSeconds());
        response.addCookie(cookie);
    }

    public void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
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
