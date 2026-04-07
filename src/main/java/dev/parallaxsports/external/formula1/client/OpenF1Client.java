package dev.parallaxsports.external.formula1.client;

import dev.parallaxsports.core.config.properties.ExternalApiProperties;
import dev.parallaxsports.external.formula1.dto.OpenF1MeetingDto;
import dev.parallaxsports.external.formula1.dto.OpenF1SessionDto;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenF1Client {

    private final RestClient.Builder restClientBuilder;
    private final ExternalApiProperties externalApiProperties;

    private RestClient restClient;

    @PostConstruct
    void init() {
        String baseUrl = externalApiProperties.getOpenf1BaseUrl();
        String resolved = (baseUrl == null || baseUrl.isBlank()) ? "https://api.openf1.org/v1" : baseUrl;
        this.restClient = restClientBuilder.baseUrl(resolved).build();
    }

    public List<OpenF1MeetingDto> fetchMeetings(int year) {
        OpenF1MeetingDto[] body;
        try {
            body = restClient
                .get()
                .uri(uriBuilder -> uriBuilder.path("/meetings").queryParam("year", year).build())
                .retrieve()
                .body(OpenF1MeetingDto[].class);
        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("OpenF1 meetings endpoint returned 404 for year={}", year);
            return Collections.emptyList();
        }

        if (body == null) {
            log.warn("OpenF1 meetings endpoint returned null body for year={}", year);
            return Collections.emptyList();
        }
        return Arrays.asList(body);
    }

    public List<OpenF1SessionDto> fetchSessions(int year) {
        OpenF1SessionDto[] body;
        try {
            body = restClient
                .get()
                .uri(uriBuilder -> uriBuilder.path("/sessions").queryParam("year", year).build())
                .retrieve()
                .body(OpenF1SessionDto[].class);
        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("OpenF1 sessions endpoint returned 404 for year={}", year);
            return Collections.emptyList();
        }

        if (body == null) {
            log.warn("OpenF1 sessions endpoint returned null body for year={}", year);
            return Collections.emptyList();
        }
        return Arrays.asList(body);
    }
}
