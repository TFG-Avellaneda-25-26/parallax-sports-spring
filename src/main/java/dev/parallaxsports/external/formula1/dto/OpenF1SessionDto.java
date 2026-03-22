package dev.parallaxsports.external.formula1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

public record OpenF1SessionDto(
    @JsonProperty("circuit_key") Long circuitKey,
    @JsonProperty("circuit_short_name") String circuitShortName,
    @JsonProperty("country_code") String countryCode,
    @JsonProperty("country_key") Long countryKey,
    @JsonProperty("country_name") String countryName,
    @JsonProperty("date_end") OffsetDateTime dateEnd,
    @JsonProperty("date_start") OffsetDateTime dateStart,
    @JsonProperty("gmt_offset") String gmtOffset,
    String location,
    @JsonProperty("meeting_key") Long meetingKey,
    @JsonProperty("session_key") Long sessionKey,
    @JsonProperty("session_name") String sessionName,
    @JsonProperty("session_type") String sessionType,
    Integer year
) {
}
