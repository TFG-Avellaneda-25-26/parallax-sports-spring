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
    private String telegramDlqStream = "alerts.telegram.dlq.v1";
    private String discordDlqStream = "alerts.discord.dlq.v1";
    private String emailDlqStream = "alerts.email.dlq.v1";
    private String telegramConsumerGroup = "alerts.telegram.workers.v1";
    private String discordConsumerGroup = "alerts.discord.workers.v1";
    private String emailConsumerGroup = "alerts.email.workers.v1";
    private long pendingClaimIdleMs = 60000;
    private boolean streamTrimEnabled = true;
    private long streamMaxLen = 200000;
    private boolean httpFallbackEnabled = false;

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

    public String getTelegramDlqStream() {
        return telegramDlqStream;
    }

    public void setTelegramDlqStream(String telegramDlqStream) {
        this.telegramDlqStream = telegramDlqStream;
    }

    public String getDiscordDlqStream() {
        return discordDlqStream;
    }

    public void setDiscordDlqStream(String discordDlqStream) {
        this.discordDlqStream = discordDlqStream;
    }

    public String getEmailDlqStream() {
        return emailDlqStream;
    }

    public void setEmailDlqStream(String emailDlqStream) {
        this.emailDlqStream = emailDlqStream;
    }

    public String getTelegramConsumerGroup() {
        return telegramConsumerGroup;
    }

    public void setTelegramConsumerGroup(String telegramConsumerGroup) {
        this.telegramConsumerGroup = telegramConsumerGroup;
    }

    public String getDiscordConsumerGroup() {
        return discordConsumerGroup;
    }

    public void setDiscordConsumerGroup(String discordConsumerGroup) {
        this.discordConsumerGroup = discordConsumerGroup;
    }

    public String getEmailConsumerGroup() {
        return emailConsumerGroup;
    }

    public void setEmailConsumerGroup(String emailConsumerGroup) {
        this.emailConsumerGroup = emailConsumerGroup;
    }

    public long getPendingClaimIdleMs() {
        return pendingClaimIdleMs;
    }

    public void setPendingClaimIdleMs(long pendingClaimIdleMs) {
        this.pendingClaimIdleMs = pendingClaimIdleMs;
    }

    public boolean isStreamTrimEnabled() {
        return streamTrimEnabled;
    }

    public void setStreamTrimEnabled(boolean streamTrimEnabled) {
        this.streamTrimEnabled = streamTrimEnabled;
    }

    public long getStreamMaxLen() {
        return streamMaxLen;
    }

    public void setStreamMaxLen(long streamMaxLen) {
        this.streamMaxLen = streamMaxLen;
    }

    public boolean isHttpFallbackEnabled() {
        return httpFallbackEnabled;
    }

    public void setHttpFallbackEnabled(boolean httpFallbackEnabled) {
        this.httpFallbackEnabled = httpFallbackEnabled;
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

    public String dlqStreamNameForChannel(String channel) {
        if ("telegram".equalsIgnoreCase(channel)) {
            return telegramDlqStream;
        }
        if ("discord".equalsIgnoreCase(channel)) {
            return discordDlqStream;
        }
        if ("email".equalsIgnoreCase(channel)) {
            return emailDlqStream;
        }
        throw new IllegalArgumentException("Unsupported alert channel: " + channel);
    }

    public String consumerGroupForChannel(String channel) {
        if ("telegram".equalsIgnoreCase(channel)) {
            return telegramConsumerGroup;
        }
        if ("discord".equalsIgnoreCase(channel)) {
            return discordConsumerGroup;
        }
        if ("email".equalsIgnoreCase(channel)) {
            return emailConsumerGroup;
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
