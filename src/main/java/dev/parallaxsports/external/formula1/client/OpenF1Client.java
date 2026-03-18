package dev.parallaxsports.external.formula1.client;

import dev.parallaxsports.core.config.properties.ExternalApiProperties;
import dev.parallaxsports.external.formula1.dto.OpenF1MeetingDto;
import dev.parallaxsports.external.formula1.dto.OpenF1SessionDto;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class OpenF1Client {

    private final RestClient.Builder restClientBuilder;
    private final ExternalApiProperties externalApiProperties;

    public List<OpenF1MeetingDto> fetchMeetings(int year) {
        OpenF1MeetingDto[] body = restClient()
            .get()
            .uri(uriBuilder -> uriBuilder.path("/meetings").queryParam("year", year).build())
            .retrieve()
            .body(OpenF1MeetingDto[].class);

        if (body == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(body);
    }

    public List<OpenF1SessionDto> fetchSessions(int year) {
        OpenF1SessionDto[] body = restClient()
            .get()
            .uri(uriBuilder -> uriBuilder.path("/sessions").queryParam("year", year).build())
            .retrieve()
            .body(OpenF1SessionDto[].class);

        if (body == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(body);
    }

    private RestClient restClient() {
        String baseUrl = externalApiProperties.getOpenf1BaseUrl();
        String resolved = (baseUrl == null || baseUrl.isBlank()) ? "https://api.openf1.org/v1" : baseUrl;
        return restClientBuilder.baseUrl(resolved).build();
    }
}
