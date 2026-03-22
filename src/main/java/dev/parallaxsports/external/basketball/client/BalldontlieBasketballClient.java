package dev.parallaxsports.external.basketball.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.parallaxsports.core.config.properties.ExternalApiProperties;
import dev.parallaxsports.core.exception.SystemConfigurationException;
import dev.parallaxsports.core.exception.UpstreamServiceException;
import dev.parallaxsports.external.basketball.dto.BalldontlieEnvelopeDto;
import dev.parallaxsports.external.basketball.dto.BalldontlieGameDto;
import dev.parallaxsports.external.basketball.dto.BalldontlieTeamDto;
import java.io.IOException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
@Slf4j
public class BalldontlieBasketballClient {

    static final int MAX_PAGE_SIZE = 100;

    private final RestClient.Builder restClientBuilder;
    private final ExternalApiProperties externalApiProperties;
    private final ObjectMapper objectMapper;
    private RestClient restClient;

// TODO I have to check if this is the best strat for caching and not rebuilding per call
    @PostConstruct
    void init() {
        this.restClient = restClientBuilder
            .baseUrl(externalApiProperties.getBalldontlieBasketballBaseUrl())
            .build();
    }

    public BalldontlieEnvelopeDto<BalldontlieGameDto> fetchGamesPage(LocalDate startDate, Long cursor) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("start_date", startDate.toString());
        params.put("per_page", String.valueOf(MAX_PAGE_SIZE));
        if (cursor != null) {
            params.put("cursor", String.valueOf(cursor));
        }
        return get("/games", params, new TypeReference<>() {});
    }

  
    public BalldontlieEnvelopeDto<BalldontlieGameDto> fetchGamesPage(LocalDate startDate, LocalDate endDate, Long cursor) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("start_date", startDate.toString());
        params.put("end_date", endDate.toString());
        params.put("per_page", String.valueOf(MAX_PAGE_SIZE));
        if (cursor != null) {
            params.put("cursor", String.valueOf(cursor));
        }
        return get("/games", params, new TypeReference<>() {});
    }

 
    public BalldontlieEnvelopeDto<BalldontlieTeamDto> fetchTeamsPage(Long cursor) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("per_page", String.valueOf(MAX_PAGE_SIZE));
        if (cursor != null) {
            params.put("cursor", String.valueOf(cursor));
        }
        return get("/teams", params, new TypeReference<>() {});
    }

    private <T> BalldontlieEnvelopeDto<T> get(
        String path,
        Map<String, String> queryParams,
        TypeReference<BalldontlieEnvelopeDto<T>> type
    ) {
        String apiKey = externalApiProperties.getBalldontlieApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new SystemConfigurationException("app.external-api.balldontlie-api-key must be configured");
        }

        try {
            String rawBody = restClient
                .get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path(path);
                    queryParams.forEach(builder::queryParam);
                    return builder.build();
                })
                .header(HttpHeaders.AUTHORIZATION, apiKey)
                .header(HttpHeaders.ACCEPT, "application/json")
                .retrieve()
                .body(String.class);

            if (rawBody == null || rawBody.isBlank()) {
                return new BalldontlieEnvelopeDto<>(List.of(), null);
            }
            return objectMapper.readValue(rawBody, type);
        } catch (IOException ex) {
            throw new UpstreamServiceException("balldontlie response parsing failed", ex);
        } catch (RestClientException ex) {
            throw new UpstreamServiceException("balldontlie request failed", ex);
        }
    }
}