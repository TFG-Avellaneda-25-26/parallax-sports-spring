package dev.parallaxsports.core.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.external-api")
@Validated
public class ExternalApiProperties {

    @NotBlank
    private String openf1BaseUrl;
    @NotBlank
    private String balldontlieBaseUrl;
    private String balldontlieApiKey;

    public String getOpenf1BaseUrl() {
        return openf1BaseUrl;
    }

    public void setOpenf1BaseUrl(String openf1BaseUrl) {
        this.openf1BaseUrl = openf1BaseUrl;
    }

    public String getBalldontlieBaseUrl() {
        return balldontlieBaseUrl;
    }

    public void setBalldontlieBaseUrl(String balldontlieBaseUrl) {
        this.balldontlieBaseUrl = balldontlieBaseUrl;
    }

    public String getBalldontlieApiKey() {
        return balldontlieApiKey;
    }

    public void setBalldontlieApiKey(String balldontlieApiKey) {
        this.balldontlieApiKey = balldontlieApiKey;
    }
}
