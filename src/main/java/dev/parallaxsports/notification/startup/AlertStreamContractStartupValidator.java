package dev.parallaxsports.notification.startup;

import dev.parallaxsports.core.config.properties.AlertProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Validates alert stream naming contract and key runtime settings at startup.
 *
 * This validator logs warnings for risky configuration while still allowing
 * startup so non-blocking environments can run with explicit diagnostics.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AlertStreamContractStartupValidator implements ApplicationRunner {

    private static final Set<String> CHANNELS = Set.of("telegram", "discord", "email");

    private final AlertProperties alertProperties;

    /**
     * Executes stream contract checks during boot.
     *
     * Scanner trigger words: startup, stream, dlq, consumer group, contract.
     *
     * @param args application args
     */
    @Override
    public void run(ApplicationArguments args) {
        Map<String, String> streamByChannel = new LinkedHashMap<>();
        Map<String, String> dlqByChannel = new LinkedHashMap<>();
        Map<String, String> groupByChannel = new LinkedHashMap<>();

        for (String channel : CHANNELS) {
            streamByChannel.put(channel, alertProperties.streamNameForChannel(channel));
            dlqByChannel.put(channel, alertProperties.dlqStreamNameForChannel(channel));
            groupByChannel.put(channel, alertProperties.consumerGroupForChannel(channel));
        }

        validateNonBlank(streamByChannel, "stream");
        validateNonBlank(dlqByChannel, "dlq stream");
        validateNonBlank(groupByChannel, "consumer group");
        validateUniqueness(streamByChannel, "stream");
        validateUniqueness(dlqByChannel, "dlq stream");
        logChannelSummary(streamByChannel, dlqByChannel, groupByChannel);

        if (alertProperties.isStreamTrimEnabled() && alertProperties.getStreamMaxLen() < 1) {
            log.warn("alerts startup validation: stream trim enabled but stream-max-len < 1; check app.alerts.stream-max-len");
        }

        if (alertProperties.getPendingClaimIdleMs() < 1000) {
            log.warn(
                "alerts startup validation: pending-claim-idle-ms={} is very low; duplicate processing risk may increase",
                alertProperties.getPendingClaimIdleMs()
            );
        }

        if (alertProperties.getKtorApiKey() == null || alertProperties.getKtorApiKey().isBlank()) {
            log.warn(
                "alerts startup validation: app.alerts.ktor-api-key is blank; internal callback endpoints will reject notification microservice callbacks"
            );
        }

        log.info(
            "alerts startup validation completed streams={} dlq={} groups={} trimEnabled={} maxLen={} pendingClaimIdleMs={}",
            streamByChannel,
            dlqByChannel,
            groupByChannel,
            alertProperties.isStreamTrimEnabled(),
            alertProperties.getStreamMaxLen(),
            alertProperties.getPendingClaimIdleMs()
        );
    }

    /**
     * Emits warning for blank values by channel.
     *
     * @param valuesByChannel per-channel config map
     * @param valueType label used in warning messages
     */
    private void validateNonBlank(Map<String, String> valuesByChannel, String valueType) {
        valuesByChannel.forEach((channel, value) -> {
            if (value == null || value.isBlank()) {
                log.warn("alerts startup validation: {} for channel '{}' is blank", valueType, channel);
            }
        });
    }

    /**
     * Emits warning when values are duplicated across channels.
     *
     * @param valuesByChannel per-channel config map
     * @param valueType label used in warning messages
     */
    private void validateUniqueness(Map<String, String> valuesByChannel, String valueType) {
        long uniqueCount = valuesByChannel.values().stream().distinct().count();
        if (uniqueCount != valuesByChannel.size()) {
            log.warn(
                "alerts startup validation: {} names contain duplicates {}; this may reduce channel isolation",
                valueType,
                valuesByChannel
            );
        }
    }

    /**
     * Logs channel-level summary table for streams, DLQ streams and groups.
     *
     * @param streamByChannel stream names by channel
     * @param dlqByChannel DLQ stream names by channel
     * @param groupByChannel consumer group names by channel
     */
    private void logChannelSummary(
        Map<String, String> streamByChannel,
        Map<String, String> dlqByChannel,
        Map<String, String> groupByChannel
    ) {
        log.info("alerts stream contract summary:");
        log.info("channel   | stream                     | dlq stream                  | consumer group");
        log.info("----------+----------------------------+-----------------------------+------------------------------");
        for (String channel : CHANNELS) {
            String stream = streamByChannel.getOrDefault(channel, "");
            String dlq = dlqByChannel.getOrDefault(channel, "");
            String group = groupByChannel.getOrDefault(channel, "");
            log.info(String.format("%-9s | %-26s | %-27s | %s", channel, stream, dlq, group));
        }
    }
}