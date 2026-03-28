package dev.parallaxsports.core.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app")
@Validated
@Getter
@Setter
public class AppProperties {

	private String frontendUrl = "http://localhost:4200";
}
