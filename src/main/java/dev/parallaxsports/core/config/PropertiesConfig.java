package dev.parallaxsports.core.config;

import dev.parallaxsports.core.config.properties.AppProperties;
import dev.parallaxsports.core.config.properties.ExternalApiProperties;
import dev.parallaxsports.core.config.properties.ExternalSyncProperties;
import dev.parallaxsports.core.config.properties.AlertProperties;
import dev.parallaxsports.core.config.properties.JwtProperties;
import dev.parallaxsports.core.config.properties.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    AppProperties.class,
    JwtProperties.class,
    AlertProperties.class,
    ExternalApiProperties.class,
    ExternalSyncProperties.class,
    RedisProperties.class
})
public class PropertiesConfig {
}
