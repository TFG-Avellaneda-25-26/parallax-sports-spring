package dev.parallaxsports.external.formula1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

public record OpenF1MeetingDto(
    @JsonProperty("circuit_key") Long circuitKey,
    @JsonProperty("circuit_info_url") String circuitInfoUrl,
    @JsonProperty("circuit_image") String circuitImage,
    @JsonProperty("circuit_short_name") String circuitShortName,
    @JsonProperty("circuit_type") String circuitType,
    @JsonProperty("country_code") String countryCode,
    @JsonProperty("country_flag") String countryFlag,
    @JsonProperty("country_key") Long countryKey,
    @JsonProperty("country_name") String countryName,
    @JsonProperty("date_end") OffsetDateTime dateEnd,
    @JsonProperty("date_start") OffsetDateTime dateStart,
    @JsonProperty("gmt_offset") String gmtOffset,
    String location,
    @JsonProperty("meeting_key") Long meetingKey,
    @JsonProperty("meeting_name") String meetingName,
    @JsonProperty("meeting_official_name") String meetingOfficialName,
    Integer year
) {
}
