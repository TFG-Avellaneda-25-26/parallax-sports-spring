package dev.parallaxsports.core.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.alerts")
public class AlertProperties {

    private boolean generationEnabled = true;
    private String defaultChannel = "telegram";
    private int defaultLeadTimeMinutes = 30;

    private boolean dispatchEnabled = true;
    private String dispatchCron = "0 * * * * *";
    private int dispatchBatchSize = 100;
    private String telegramStream = "alerts.telegram.v1";
    private String discordStream = "alerts.discord.v1";
    private String emailStream = "alerts.email.v1";

    private String ktorBaseUrl;
    private String ktorDispatchPath = "/internal/alerts/dispatch";
    private String ktorApiKey;

    public boolean isGenerationEnabled() {
        return generationEnabled;
    }

    public void setGenerationEnabled(boolean generationEnabled) {
        this.generationEnabled = generationEnabled;
    }

    public String getDefaultChannel() {
        return defaultChannel;
    }

    public void setDefaultChannel(String defaultChannel) {
        this.defaultChannel = defaultChannel;
    }

    public int getDefaultLeadTimeMinutes() {
        return defaultLeadTimeMinutes;
    }

    public void setDefaultLeadTimeMinutes(int defaultLeadTimeMinutes) {
        this.defaultLeadTimeMinutes = defaultLeadTimeMinutes;
    }

    public boolean isDispatchEnabled() {
        return dispatchEnabled;
    }

    public void setDispatchEnabled(boolean dispatchEnabled) {
        this.dispatchEnabled = dispatchEnabled;
    }

    public String getDispatchCron() {
        return dispatchCron;
    }

    public void setDispatchCron(String dispatchCron) {
        this.dispatchCron = dispatchCron;
    }

    public int getDispatchBatchSize() {
        return dispatchBatchSize;
    }

    public void setDispatchBatchSize(int dispatchBatchSize) {
        this.dispatchBatchSize = dispatchBatchSize;
    }

    public String getTelegramStream() {
        return telegramStream;
    }

    public void setTelegramStream(String telegramStream) {
        this.telegramStream = telegramStream;
    }

    public String getDiscordStream() {
        return discordStream;
    }

    public void setDiscordStream(String discordStream) {
        this.discordStream = discordStream;
    }

    public String getEmailStream() {
        return emailStream;
    }

    public void setEmailStream(String emailStream) {
        this.emailStream = emailStream;
    }

    public String streamNameForChannel(String channel) {
        if ("telegram".equalsIgnoreCase(channel)) {
            return telegramStream;
        }
        if ("discord".equalsIgnoreCase(channel)) {
            return discordStream;
        }
        if ("email".equalsIgnoreCase(channel)) {
            return emailStream;
        }
        throw new IllegalArgumentException("Unsupported alert channel: " + channel);
    }

    public String getKtorBaseUrl() {
        return ktorBaseUrl;
    }

    public void setKtorBaseUrl(String ktorBaseUrl) {
        this.ktorBaseUrl = ktorBaseUrl;
    }

    public String getKtorDispatchPath() {
        return ktorDispatchPath;
    }

    public void setKtorDispatchPath(String ktorDispatchPath) {
        this.ktorDispatchPath = ktorDispatchPath;
    }

    public String getKtorApiKey() {
        return ktorApiKey;
    }

    public void setKtorApiKey(String ktorApiKey) {
        this.ktorApiKey = ktorApiKey;
    }
}
