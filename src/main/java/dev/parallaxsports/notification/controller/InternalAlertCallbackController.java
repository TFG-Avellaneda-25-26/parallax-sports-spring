package dev.parallaxsports.notification.controller;

import dev.parallaxsports.notification.dto.AlertArtifactCallbackRequest;
import dev.parallaxsports.notification.dto.AlertWorkerStatusCallbackRequest;
import dev.parallaxsports.notification.service.AlertCallbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/alerts")
@RequiredArgsConstructor
public class InternalAlertCallbackController {

    private final AlertCallbackService alertCallbackService;

    @PostMapping("/{alertId}/status")
    public ResponseEntity<Void> updateStatus(
        @PathVariable Long alertId,
        @RequestHeader(name = "X-Api-Key", required = false) String apiKey,
        @RequestBody AlertWorkerStatusCallbackRequest request
    ) {
        alertCallbackService.processStatusCallback(alertId, apiKey, request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{alertId}/artifact")
    public ResponseEntity<Void> registerArtifact(
        @PathVariable Long alertId,
        @RequestHeader(name = "X-Api-Key", required = false) String apiKey,
        @RequestBody AlertArtifactCallbackRequest request
    ) {
        alertCallbackService.processArtifactCallback(alertId, apiKey, request);
        return ResponseEntity.accepted().build();
    }
}
