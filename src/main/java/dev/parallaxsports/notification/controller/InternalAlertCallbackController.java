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

/**
 * Internal callback endpoints used by the notification microservice to update
 * alert lifecycle state.
 *
 * Endpoints are API-key protected and accept asynchronous status updates plus
 * artifact updates (artifact = generated media asset metadata such as image URL).
 */
@RestController
@RequestMapping("/api/internal/alerts")
@RequiredArgsConstructor
public class InternalAlertCallbackController {

    private final AlertCallbackService alertCallbackService;

        /*============================================================
            STATUS CALLBACK
            Notification microservice delivery status updates
        ============================================================*/

        // -> Triggers: notification microservice reports delivery status || Returns: Accepted (202)
        /**
         * Accepts delivery status callback for one alert.
         *
         * @param alertId alert identifier
         * @param apiKey callback API key from notification microservice
         * @param request callback status payload
         * @return accepted response after validation and processing
         */
    @PostMapping("/{alertId}/status")
    public ResponseEntity<Void> updateStatus(
        @PathVariable Long alertId,
        @RequestHeader(name = "X-Api-Key", required = false) String apiKey,
        @RequestBody AlertWorkerStatusCallbackRequest request
    ) {
        alertCallbackService.processStatusCallback(alertId, apiKey, request);
        return ResponseEntity.accepted().build();
    }

        /*============================================================
            ARTIFACT CALLBACK
            Notification microservice artifact registration updates
        ============================================================*/

        // -> Triggers: notification microservice registers generated media metadata for alert || Returns: Accepted (202)
        /**
         * Accepts artifact callback for one alert.
         *
         * Artifact means metadata for generated media (for example a rendered
         * image URL + storage identifiers) used by media-rich channels.
         *
         * @param alertId alert identifier
         * @param apiKey callback API key from notification microservice
         * @param request artifact callback payload
         * @return accepted response after validation and processing
         */
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
