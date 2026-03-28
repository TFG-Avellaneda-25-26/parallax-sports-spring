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

    // Nueva configuración para pandascore
    private int pandascoreDefaultPerPage = 20;
    private int pandascoreDefaultPages = 2;
    private int pandascoreMaxPages = 5;
    private int pandascoreMaxPerPage = 50;
    private int pandascoreMaxRetries = 3;
    private long pandascoreBaseBackoffMillis = 500L;

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
}
