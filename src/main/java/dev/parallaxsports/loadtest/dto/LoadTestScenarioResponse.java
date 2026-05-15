package dev.parallaxsports.loadtest.dto;

public record LoadTestScenarioResponse(
    String id,
    String name,
    String description,
    Integer defaultVus,
    String defaultDuration
) {
}
