package dev.parallaxsports.sport.event.controller;

import dev.parallaxsports.core.exception.BadRequestException;
import dev.parallaxsports.sport.event.dto.EventFeedResponse;
import dev.parallaxsports.sport.event.service.EventFeedService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Validated
public class EventController {

    private final EventFeedService eventFeedService;

    @GetMapping
    public ResponseEntity<EventFeedResponse> getEvents(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(required = false) Long after,
        @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size
    ) {
        if (to.isBefore(from)) {
            throw new BadRequestException("'to' must not be before 'from'");
        }

        OffsetDateTime fromUtc = from.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime toUtc = to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC).minusNanos(1);

        EventFeedResponse feed = eventFeedService.getFeed(fromUtc, toUtc, after, size);

        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
            .body(feed);
    }
}
