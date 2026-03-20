package dev.parallaxsports.notification.service.callback;

import dev.parallaxsports.core.config.properties.AlertProperties;
import dev.parallaxsports.core.exception.SystemConfigurationException;
import dev.parallaxsports.core.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Validates shared API key protection for internal callback endpoints.
 */
@Component
@RequiredArgsConstructor
public class AlertCallbackAuthenticator {

    private static final String X_API_KEY = "X-Api-Key";

    private final AlertProperties alertProperties;

    /**
     * Validates callback API key against configured shared secret.
     *
     * @param providedApiKey key supplied by callback caller
     */
    public void validate(String providedApiKey) {
        String expectedApiKey = alertProperties.getKtorApiKey();
        if (expectedApiKey == null || expectedApiKey.isBlank()) {
            throw new SystemConfigurationException("Missing server callback key configuration");
        }
        if (providedApiKey == null || !expectedApiKey.equals(providedApiKey)) {
            throw new UnauthorizedException("Invalid " + X_API_KEY);
        }
    }
}