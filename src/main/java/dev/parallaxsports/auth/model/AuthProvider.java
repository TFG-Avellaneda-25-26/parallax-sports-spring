package dev.parallaxsports.auth.model;

import lombok.AllArgsConstructor;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Arrays;

@AllArgsConstructor
public enum AuthProvider {
    GOOGLE("google", "sub", "name"),
    DISCORD("discord", "id", "username");
    //GITHUB("github", "id", "login");

    private final String registrationId;
    private final String subjectAttribute;
    private final String usernameAttribute;

    public String getSubject(OAuth2User oAuth2User) {
        Object attr = oAuth2User.getAttribute(subjectAttribute);
        return attr != null ? attr.toString() : null;
    }

    public String getUsername(OAuth2User oAuth2User) {
        Object attr = oAuth2User.getAttribute(usernameAttribute);
        return attr != null ? attr.toString() : null;
    }

    public static AuthProvider fromId(String id) {
        return Arrays.stream(values())
                .filter(p -> p.registrationId.equalsIgnoreCase(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Provider not supported" + id));
    }

}
