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

    // Nueva configuración para pandascore
    private int pandascoreDefaultPerPage = 20;
    private int pandascoreDefaultPages = 2;
    private int pandascoreMaxPages = 5;
    private int pandascoreMaxPerPage = 50;
    private int pandascoreMaxRetries = 3;
    private long pandascoreBaseBackoffMillis = 500L;
    // Base URL y API key para PandaScore (opcional en configuración; hay fallback en cliente)
    private String pandascoreBaseUrl;
    private String pandascoreApiKey;

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

    // Getters / Setters for new properties
    public int getPandascoreDefaultPerPage() {
        return pandascoreDefaultPerPage;
    }

    public void setPandascoreDefaultPerPage(int pandascoreDefaultPerPage) {
        this.pandascoreDefaultPerPage = pandascoreDefaultPerPage;
    }

    public int getPandascoreDefaultPages() {
        return pandascoreDefaultPages;
    }

    public void setPandascoreDefaultPages(int pandascoreDefaultPages) {
        this.pandascoreDefaultPages = pandascoreDefaultPages;
    }

    public int getPandascoreMaxPages() {
        return pandascoreMaxPages;
    }

    public void setPandascoreMaxPages(int pandascoreMaxPages) {
        this.pandascoreMaxPages = pandascoreMaxPages;
    }

    public int getPandascoreMaxPerPage() {
        return pandascoreMaxPerPage;
    }

    public void setPandascoreMaxPerPage(int pandascoreMaxPerPage) {
        this.pandascoreMaxPerPage = pandascoreMaxPerPage;
    }

    public int getPandascoreMaxRetries() {
        return pandascoreMaxRetries;
    }

    public void setPandascoreMaxRetries(int pandascoreMaxRetries) {
        this.pandascoreMaxRetries = pandascoreMaxRetries;
    }

    public long getPandascoreBaseBackoffMillis() {
        return pandascoreBaseBackoffMillis;
    }

    public void setPandascoreBaseBackoffMillis(long pandascoreBaseBackoffMillis) {
        this.pandascoreBaseBackoffMillis = pandascoreBaseBackoffMillis;
    }

    public String getPandascoreBaseUrl() {
        return pandascoreBaseUrl;
    }

    public void setPandascoreBaseUrl(String pandascoreBaseUrl) {
        this.pandascoreBaseUrl = pandascoreBaseUrl;
    }

    public String getPandascoreApiKey() {
        return pandascoreApiKey;
    }

    public void setPandascoreApiKey(String pandascoreApiKey) {
        this.pandascoreApiKey = pandascoreApiKey;
    }
}
