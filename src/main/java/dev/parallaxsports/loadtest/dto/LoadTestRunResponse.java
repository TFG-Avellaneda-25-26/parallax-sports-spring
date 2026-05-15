package dev.parallaxsports.loadtest.dto;

import dev.parallaxsports.loadtest.model.LoadTestRun;
import java.time.OffsetDateTime;
import java.util.Map;

public record LoadTestRunResponse(
    String runUuid,
    String scenarioId,
    String status,
    Integer exitCode,
    Long startedByUserId,
    OffsetDateTime startedAt,
    OffsetDateTime finishedAt,
    Integer vus,
    String duration,
    Map<String, Object> summaryJson
) {
    public static LoadTestRunResponse from(LoadTestRun e) {
        return new LoadTestRunResponse(
            e.getRunUuid(),
            e.getScenarioId(),
            e.getStatus(),
            e.getExitCode(),
            e.getStartedByUserId(),
            e.getStartedAt(),
            e.getFinishedAt(),
            e.getVus(),
            e.getDuration(),
            e.getSummaryJson()
        );
    }
}
