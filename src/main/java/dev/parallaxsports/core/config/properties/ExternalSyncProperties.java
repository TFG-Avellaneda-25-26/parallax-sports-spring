package dev.parallaxsports.core.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.external-sync")
@Validated
public class ExternalSyncProperties {

    private boolean enabled = true;
    private String zoneId = "UTC";
    private String dailyCron = "0 30 0 * * *";
    private int yearsBack = 0;
    private int yearsForward = 1;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public String getDailyCron() {
        return dailyCron;
    }

    public void setDailyCron(String dailyCron) {
        this.dailyCron = dailyCron;
    }

    public int getYearsBack() {
        return yearsBack;
    }

    public void setYearsBack(int yearsBack) {
        this.yearsBack = yearsBack;
    }

    public int getYearsForward() {
        return yearsForward;
    }

    public void setYearsForward(int yearsForward) {
        this.yearsForward = yearsForward;
    }
}
