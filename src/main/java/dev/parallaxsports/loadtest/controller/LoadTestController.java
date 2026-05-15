package dev.parallaxsports.loadtest.controller;

import dev.parallaxsports.core.exception.ResourceNotFoundException;
import dev.parallaxsports.loadtest.dto.LoadTestRunResponse;
import dev.parallaxsports.loadtest.dto.LoadTestScenarioResponse;
import dev.parallaxsports.loadtest.dto.LoadTestStartRequest;
import dev.parallaxsports.loadtest.repository.LoadTestRunRepository;
import dev.parallaxsports.loadtest.service.LoadTestRunnerService;
import dev.parallaxsports.user.model.User;
import dev.parallaxsports.user.repository.UserRepository;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/admin/loadtest")
@RequiredArgsConstructor
public class LoadTestController {

    private final LoadTestRunnerService runner;
    private final LoadTestRunRepository repository;
    private final UserRepository userRepository;

    @GetMapping("/scenarios")
    public ResponseEntity<List<LoadTestScenarioResponse>> scenarios() {
        return ResponseEntity.ok(runner.listScenarios());
    }

    @PostMapping("/runs")
    public ResponseEntity<LoadTestRunResponse> start(
        @Valid @RequestBody LoadTestStartRequest request,
        @AuthenticationPrincipal UserDetails actor
    ) {
        Long actorId = actor != null
            ? userRepository.findByEmail(actor.getUsername()).map(User::getId).orElse(null)
            : null;
        return ResponseEntity.ok(LoadTestRunResponse.from(runner.start(request, actorId)));
    }

    @GetMapping("/runs")
    public ResponseEntity<Page<LoadTestRunResponse>> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
        return ResponseEntity.ok(
            repository.findAllByOrderByStartedAtDesc(PageRequest.of(page, Math.min(size, 200)))
                .map(LoadTestRunResponse::from)
        );
    }

    @GetMapping("/runs/{runUuid}")
    public ResponseEntity<LoadTestRunResponse> get(@PathVariable String runUuid) {
        return repository.findByRunUuid(runUuid)
            .map(LoadTestRunResponse::from)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new ResourceNotFoundException("Run not found: " + runUuid));
    }

    @PostMapping("/runs/{runUuid}/stop")
    public ResponseEntity<LoadTestRunResponse> stop(
        @PathVariable String runUuid,
        @AuthenticationPrincipal UserDetails actor
    ) {
        Long actorId = actor != null
            ? userRepository.findByEmail(actor.getUsername()).map(User::getId).orElse(null)
            : null;
        return ResponseEntity.ok(LoadTestRunResponse.from(runner.stop(runUuid, actorId)));
    }

    @GetMapping(value = "/runs/{runUuid}/logs", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter logs(@PathVariable String runUuid) throws IOException {
        SseEmitter emitter = new SseEmitter(0L);
        InputStream stream = runner.streamLogs(runUuid);
        Executors.newSingleThreadExecutor().submit(() -> {
            byte[] buf = new byte[4096];
            try {
                int n;
                while ((n = stream.read(buf)) != -1) {
                    emitter.send(new String(buf, 0, n));
                }
                emitter.complete();
            } catch (IOException ex) {
                emitter.completeWithError(ex);
            } finally {
                try { stream.close(); } catch (IOException ignored) {}
            }
        });
        return emitter;
    }
}
