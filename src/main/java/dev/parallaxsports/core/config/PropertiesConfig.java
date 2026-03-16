package dev.parallaxsports.core.config;

import dev.parallaxsports.core.config.properties.DiscordProperties;
import dev.parallaxsports.core.config.properties.ExternalApiProperties;
import dev.parallaxsports.core.config.properties.JwtProperties;
import dev.parallaxsports.core.config.properties.RedisProperties;
import dev.parallaxsports.core.config.properties.TelegramProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    JwtProperties.class,
    DiscordProperties.class,
    TelegramProperties.class,
    ExternalApiProperties.class,
    RedisProperties.class
})
public class PropertiesConfig {
}
