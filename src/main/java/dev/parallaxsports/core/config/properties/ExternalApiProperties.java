package dev.parallaxsports.core.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.external-api")
@Validated
public class ExternalApiProperties {

    private String openf1BaseUrl;
    private String pandascoreBaseUrl;
    private String apifootballBaseUrl;
    private String apifootballApiKey;
    private String pandascoreApiKey;

    public String getOpenf1BaseUrl() {
        return openf1BaseUrl;
    }

    public void setOpenf1BaseUrl(String openf1BaseUrl) {
        this.openf1BaseUrl = openf1BaseUrl;
    }

    public String getPandascoreBaseUrl() {
        return pandascoreBaseUrl;
    }

    public void setPandascoreBaseUrl(String pandascoreBaseUrl) {
        this.pandascoreBaseUrl = pandascoreBaseUrl;
    }

    public String getApifootballBaseUrl() {
        return apifootballBaseUrl;
    }

    public void setApifootballBaseUrl(String apifootballBaseUrl) {
        this.apifootballBaseUrl = apifootballBaseUrl;
    }

    public String getApifootballApiKey() {
        return apifootballApiKey;
    }

    public void setApifootballApiKey(String apifootballApiKey) {
        this.apifootballApiKey = apifootballApiKey;
    }

    public String getPandascoreApiKey() {
        return pandascoreApiKey;
    }

    public void setPandascoreApiKey(String pandascoreApiKey) {
        this.pandascoreApiKey = pandascoreApiKey;
    }
}
