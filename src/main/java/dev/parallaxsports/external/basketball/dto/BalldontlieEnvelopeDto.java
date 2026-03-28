package dev.parallaxsports.external.basketball.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BalldontlieEnvelopeDto<T>(
    List<T> data,
    BalldontlieMetaDto meta
) {
}