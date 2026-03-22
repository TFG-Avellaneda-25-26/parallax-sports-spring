package dev.parallaxsports.core.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.alerts")
@Validated
public class AlertProperties {

    // ── Constants: implementation details that don't change per environment ──

    private static final String DEFAULT_CHANNEL = "telegram";
    private static final int DEFAULT_LEAD_TIME_MINUTES = 30;
    private static final int DISPATCH_BATCH_SIZE = 100;
    private static final long PENDING_CLAIM_IDLE_MS = 60_000;
    private static final String KTOR_DISPATCH_PATH = "/internal/alerts/dispatch";

    private static final String TELEGRAM_STREAM = "alerts.telegram.v1";
    private static final String DISCORD_STREAM = "alerts.discord.v1";
    private static final String EMAIL_STREAM = "alerts.email.v1";
    private static final String TELEGRAM_DLQ_STREAM = "alerts.telegram.dlq.v1";
    private static final String DISCORD_DLQ_STREAM = "alerts.discord.dlq.v1";
    private static final String EMAIL_DLQ_STREAM = "alerts.email.dlq.v1";
    private static final String TELEGRAM_CONSUMER_GROUP = "alerts.telegram.workers.v1";
    private static final String DISCORD_CONSUMER_GROUP = "alerts.discord.workers.v1";
    private static final String EMAIL_CONSUMER_GROUP = "alerts.email.workers.v1";

    // ── Externalized: toggled/tuned per environment ──

    private boolean generationEnabled = true;
    private boolean dispatchEnabled = true;
    @NotBlank
    private String dispatchCron = "0 * * * * *";
    private boolean streamTrimEnabled = true;
    private long streamMaxLen = 200_000;
    private boolean httpFallbackEnabled = false;
    private String ktorBaseUrl;
    private String ktorApiKey;

    // ── Constant accessors ──

    public String getDefaultChannel() {
        return DEFAULT_CHANNEL;
    }

    public int getDefaultLeadTimeMinutes() {
        return DEFAULT_LEAD_TIME_MINUTES;
    }

    public int getDispatchBatchSize() {
        return DISPATCH_BATCH_SIZE;
    }

    public long getPendingClaimIdleMs() {
        return PENDING_CLAIM_IDLE_MS;
    }

    public String getKtorDispatchPath() {
        return KTOR_DISPATCH_PATH;
    }

    // ── Externalized accessors ──

    public boolean isGenerationEnabled() {
        return generationEnabled;
    }

    public void setGenerationEnabled(boolean generationEnabled) {
        this.generationEnabled = generationEnabled;
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

    public String getKtorBaseUrl() {
        return ktorBaseUrl;
    }

    public void setKtorBaseUrl(String ktorBaseUrl) {
        this.ktorBaseUrl = ktorBaseUrl;
    }

    public String getKtorApiKey() {
        return ktorApiKey;
    }

    public void setKtorApiKey(String ktorApiKey) {
        this.ktorApiKey = ktorApiKey;
    }

    // ── Channel routing ──

    public String streamNameForChannel(String channel) {
        return switch (channel.toLowerCase()) {
            case "telegram" -> TELEGRAM_STREAM;
            case "discord" -> DISCORD_STREAM;
            case "email" -> EMAIL_STREAM;
            default -> throw new IllegalArgumentException("Unsupported alert channel: " + channel);
        };
    }

    public String dlqStreamNameForChannel(String channel) {
        return switch (channel.toLowerCase()) {
            case "telegram" -> TELEGRAM_DLQ_STREAM;
            case "discord" -> DISCORD_DLQ_STREAM;
            case "email" -> EMAIL_DLQ_STREAM;
            default -> throw new IllegalArgumentException("Unsupported alert channel: " + channel);
        };
    }

    public String consumerGroupForChannel(String channel) {
        return switch (channel.toLowerCase()) {
            case "telegram" -> TELEGRAM_CONSUMER_GROUP;
            case "discord" -> DISCORD_CONSUMER_GROUP;
            case "email" -> EMAIL_CONSUMER_GROUP;
            default -> throw new IllegalArgumentException("Unsupported alert channel: " + channel);
        };
    }
}
