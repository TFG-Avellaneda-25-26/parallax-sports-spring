package dev.parallaxsports.loadtest.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import dev.parallaxsports.audit.service.AuditService;
import dev.parallaxsports.core.exception.ResourceNotFoundException;
import dev.parallaxsports.loadtest.dto.LoadTestScenarioResponse;
import dev.parallaxsports.loadtest.dto.LoadTestStartRequest;
import dev.parallaxsports.loadtest.model.LoadTestRun;
import dev.parallaxsports.loadtest.repository.LoadTestRunRepository;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoadTestRunnerService {

    private final DockerClient docker;
    private final LoadTestRunRepository repository;
    private final AuditService auditService;

    @Value("${app.loadtest.k6-image:grafana/k6:latest}")
    private String k6Image;

    @Value("${app.loadtest.network:stack}")
    private String network;

    @Value("${app.loadtest.scripts-volume:/loadtests}")
    private String scriptsVolumeHostPath;

    @Value("${app.loadtest.prom-rw-url:http://prometheus:9090/api/v1/write}")
    private String promRemoteWriteUrl;

    @Value("${app.loadtest.max-concurrent:2}")
    private int maxConcurrent;

    private final Map<String, ScenarioDef> scenarios = new ConcurrentHashMap<>();

    @PostConstruct
    void registerScenarios() {
        register("auth_smoke", "Auth smoke test", "1 VU register/login/refresh/logout sanity check", 1, "30s");
        register("auth_login_stress", "Auth login stress", "Ramp to 200 VUs, sustain 5 min on POST /api/auth/login", 200, "7m");
        register("jwt_blacklist_stress", "JWT blacklist stress", "Hammer blacklisted access tokens — validates hybrid Redis/Postgres revocation under load", 100, "3m");
        register("events_read_stress", "Events read stress", "500 VU read load on public sport endpoints", 500, "5m");
        register("follows_write_stress", "Follows write stress", "50 VU follows create/delete + notification channel updates", 50, "10m");
        register("alerts_pipeline_e2e", "Alerts pipeline E2E", "Trigger alert generation → poll audit log until sent", 10, "5m");
        register("alerts_stream_isolated", "Alerts stream isolated (xk6-redis)", "XADD synthetic alerts directly to Redis — needs xk6-redis image", 1, "5m");
        register("playwright_render_stress", "Playwright render stress", "Direct HTTP load on ms-playwright /api/internal/screenshot", 50, "3m");
        register("spike_recovery", "Spike recovery", "50 → 500 → 50 VU spike pattern", 500, "61m");
    }

    private void register(String id, String name, String desc, int vus, String duration) {
        scenarios.put(id, new ScenarioDef(id, name, desc, vus, duration));
    }

    public List<LoadTestScenarioResponse> listScenarios() {
        List<LoadTestScenarioResponse> out = new ArrayList<>();
        for (ScenarioDef s : scenarios.values()) {
            out.add(new LoadTestScenarioResponse(s.id, s.name, s.description, s.vus, s.duration));
        }
        return out;
    }

    public synchronized LoadTestRun start(LoadTestStartRequest request, Long actorUserId) {
        ScenarioDef scenario = scenarios.get(request.scenarioId());
        if (scenario == null) {
            throw new ResourceNotFoundException("Scenario not found: " + request.scenarioId());
        }

        int activeRuns = repository.findByStatus("running").size();
        if (activeRuns >= maxConcurrent) {
            throw new IllegalStateException("Too many concurrent load tests (" + activeRuns + "/" + maxConcurrent + ")");
        }

        int vus = request.vus() != null ? request.vus() : scenario.vus;
        String duration = request.duration() != null ? request.duration() : scenario.duration;
        String runUuid = UUID.randomUUID().toString();

        List<String> env = new ArrayList<>();
        env.add("K6_VUS=" + vus);
        env.add("K6_DURATION=" + duration);
        env.add("K6_PROMETHEUS_RW_SERVER_URL=" + promRemoteWriteUrl);
        env.add("RUN_UUID=" + runUuid);
        if (request.envOverrides() != null) {
            request.envOverrides().forEach((k, v) -> env.add(k + "=" + v));
        }

        Volume scriptsMount = new Volume("/scripts");
        HostConfig hostConfig = HostConfig.newHostConfig()
            .withNetworkMode(network)
            .withBinds(new Bind(scriptsVolumeHostPath, scriptsMount, AccessMode.ro));

        CreateContainerResponse created = docker.createContainerCmd(k6Image)
            .withName("loadtest-" + runUuid)
            .withLabels(Map.of("parallax.loadtest.runId", runUuid))
            .withEnv(env)
            .withCmd("run", "--out", "experimental-prometheus-rw", "/scripts/" + scenario.id + ".js")
            .withHostConfig(hostConfig)
            .exec();

        docker.startContainerCmd(created.getId()).exec();

        LoadTestRun run = LoadTestRun.builder()
            .runUuid(runUuid)
            .scenarioId(scenario.id)
            .containerId(created.getId())
            .startedByUserId(actorUserId)
            .status("running")
            .vus(vus)
            .duration(duration)
            .build();
        repository.save(run);

        auditService.record("LOADTEST_START", actorUserId, "load_test_run", run.getId(),
            Map.of("scenario", scenario.id, "vus", vus, "duration", duration, "runUuid", runUuid));

        log.info("Started load test {} ({}) container={}", scenario.id, runUuid, created.getId());
        return run;
    }

    public LoadTestRun stop(String runUuid, Long actorUserId) {
        LoadTestRun run = repository.findByRunUuid(runUuid)
            .orElseThrow(() -> new ResourceNotFoundException("Run not found: " + runUuid));

        if (!"running".equals(run.getStatus())) {
            return run;
        }

        try {
            docker.stopContainerCmd(run.getContainerId()).withTimeout(10).exec();
        } catch (NotFoundException ex) {
            log.warn("Container {} already gone", run.getContainerId());
        }

        run.setStatus("stopped");
        run.setFinishedAt(OffsetDateTime.now());
        repository.save(run);

        auditService.record("LOADTEST_STOP", actorUserId, "load_test_run", run.getId(),
            Map.of("runUuid", runUuid));

        return run;
    }

    public PipedInputStream streamLogs(String runUuid) throws IOException {
        LoadTestRun run = repository.findByRunUuid(runUuid)
            .orElseThrow(() -> new ResourceNotFoundException("Run not found: " + runUuid));

        PipedInputStream in = new PipedInputStream(64 * 1024);
        PipedOutputStream out = new PipedOutputStream(in);

        docker.logContainerCmd(run.getContainerId())
            .withStdOut(true)
            .withStdErr(true)
            .withFollowStream(true)
            .withTailAll()
            .exec(new LogContainerResultCallback() {
                @Override
                public void onNext(com.github.dockerjava.api.model.Frame frame) {
                    try {
                        out.write(frame.getPayload());
                        out.flush();
                    } catch (IOException ignored) {
                        try { close(); } catch (IOException ignoredToo) {}
                    }
                }
            });

        return in;
    }

    private record ScenarioDef(String id, String name, String description, int vus, String duration) {}
}
