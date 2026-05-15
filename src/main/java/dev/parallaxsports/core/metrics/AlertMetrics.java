package dev.parallaxsports.core.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlertMetrics {

    private final MeterRegistry registry;

    public void generated(String sport) {
        Counter.builder("alerts.generated.total")
            .tag("sport", sport == null ? "unknown" : sport)
            .register(registry)
            .increment();
    }

    public void created(String channel) {
        Counter.builder("alerts.created.total")
            .tag("channel", channel == null ? "unknown" : channel)
            .register(registry)
            .increment();
    }

    public void queued(String channel) {
        Counter.builder("alerts.queued.total")
            .tag("channel", channel)
            .register(registry)
            .increment();
    }

    public void statusCallback(String channel, String status) {
        Counter.builder("alerts.status.callback.total")
            .tag("channel", channel)
            .tag("status", status)
            .register(registry)
            .increment();
    }

    public void recordPipelineLatency(String channel, String stage, Duration duration) {
        Timer.builder("alerts.pipeline.latency")
            .tag("channel", channel)
            .tag("stage", stage)
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry)
            .record(duration);
    }
}
