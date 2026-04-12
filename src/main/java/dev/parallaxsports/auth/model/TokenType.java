package dev.parallaxsports.auth.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TokenType {
    ACCESS_TOKEN("access_token", 1800),
    REFRESH_TOKEN("refresh_token", 604800);

    private final String cookieName;
    private final int expiration;
}
