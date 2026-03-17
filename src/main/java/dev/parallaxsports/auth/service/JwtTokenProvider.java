package dev.parallaxsports.auth.service;

import dev.parallaxsports.core.config.properties.JwtProperties;
import dev.parallaxsports.user.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.SecretKey;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    // token_type lets us enforce that refresh tokens cannot be used as access tokens.
    private static final String TOKEN_TYPE_CLAIM = "token_type";
    // role claim is included for downstream debugging/inspection; DB remains source of truth.
    private static final String ROLE_CLAIM = "role";
    private SecretKey signingKey;

    @PostConstruct
    void init() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes for HS256");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        log.info("JWT signing key initialized (HS256)");
    }

    public String issueAccessToken(User user) {
        return issueToken(user, "access", jwtProperties.getAccessTokenExpirationSeconds());
    }

    public String issueRefreshToken(User user) {
        return issueToken(user, "refresh", jwtProperties.getRefreshTokenExpirationSeconds());
    }

    public String extractSubject(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isAccessToken(String token) {
        return "access".equals(parseClaims(token).get(TOKEN_TYPE_CLAIM, String.class));
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(parseClaims(token).get(TOKEN_TYPE_CLAIM, String.class));
    }

    public boolean isTokenValid(String token, UserDetails userDetails, String expectedType) {
        try {
            return isTokenValid(parseClaims(token), userDetails, expectedType);
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean isTokenValid(Claims claims, UserDetails userDetails, String expectedType) {
        // Parse once, then validate identity + type + expiry.
        String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
        Date expiration = claims.getExpiration();
        return claims.getSubject().equals(userDetails.getUsername())
            && expectedType.equals(tokenType)
            && expiration != null
            && expiration.after(new Date());
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private String issueToken(User user, String tokenType, long ttlSeconds) {
        Date issuedAt = new Date();
        Date expiresAt = new Date(issuedAt.getTime() + ttlSeconds * 1000);

        // Subject=email aligns with login identifier and UserDetailsService lookup.
        return Jwts.builder()
            .subject(user.getEmail())
            .claim(TOKEN_TYPE_CLAIM, tokenType)
            .claim(ROLE_CLAIM, user.getRole().name())
            .issuedAt(issuedAt)
            .expiration(expiresAt)
            .signWith(signingKey, Jwts.SIG.HS256)
            .compact();
    }
}
