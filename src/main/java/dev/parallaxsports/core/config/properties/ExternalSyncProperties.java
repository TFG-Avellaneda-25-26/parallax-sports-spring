package dev.parallaxsports.core.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.external-sync")
@Validated
public class ExternalSyncProperties {

    private boolean enabled = true;
    @NotBlank
    private String zoneId = "UTC";
    @NotBlank
    private String dailyCron = "0 30 0 * * *";
    @Min(0)
    private int yearsBack = 0;
    @Min(0)
    private int yearsForward = 1;
    private boolean nbaEnabled = true;
    private boolean wnbaEnabled = true;

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

    public boolean isNbaEnabled() {
        return nbaEnabled;
    }

    public void setNbaEnabled(boolean nbaEnabled) {
        this.nbaEnabled = nbaEnabled;
    }

    public boolean isWnbaEnabled() {
        return wnbaEnabled;
    }

    public void setWnbaEnabled(boolean wnbaEnabled) {
        this.wnbaEnabled = wnbaEnabled;
    }

}
