package dev.parallaxsports.loadtest.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record LoadTestStartRequest(
    @NotBlank String scenarioId,
    Integer vus,
    String duration,
    Map<String, String> envOverrides
) {
}
