package dev.parallaxsports.notification.controller;

import dev.parallaxsports.notification.service.AlertRenderService;
import dev.parallaxsports.notification.service.AlertRenderService.RenderResult;
import dev.parallaxsports.notification.service.callback.AlertCallbackAuthenticator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/render")
@RequiredArgsConstructor
public class AlertRenderController {

    private final AlertRenderService renderService;
    private final AlertCallbackAuthenticator authenticator;

    @GetMapping(value = "/event/{eventId}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> renderEvent(
        @PathVariable Long eventId,
        @RequestParam(name = "channel", defaultValue = "discord") String channel,
        @RequestParam(name = "tz", required = false) String tz,
        @RequestParam(name = "locale", required = false) String locale,
        @RequestHeader(name = "X-Api-Key", required = false) String apiKey
    ) {
        authenticator.validate(apiKey);
        RenderResult result = renderService.render(eventId, channel, tz, locale);
        return ResponseEntity.ok()
            .header("X-Render-Hash", result.hash())
            .contentType(MediaType.valueOf("text/html; charset=utf-8"))
            .body(result.html());
    }
}
