package dev.parallaxsports.core.config.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.discord")
@Validated
@Getter
@Setter
public class DiscordProperties {

    @NotBlank
    private String botToken;
}

