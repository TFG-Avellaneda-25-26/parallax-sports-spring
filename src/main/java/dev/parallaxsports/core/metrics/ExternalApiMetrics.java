package dev.parallaxsports.core.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExternalApiMetrics {

    private final MeterRegistry registry;

    public void recordCall(String api, String endpoint, String outcome, Duration duration) {
        timer(api, endpoint, outcome).record(duration);
    }

    public Timer.Sample start() {
        return Timer.start(registry);
    }

    public <T> T time(String api, String endpoint, Supplier<T> call) {
        Timer.Sample sample = Timer.start(registry);
        String outcome = "success";
        try {
            return call.get();
        } catch (RuntimeException ex) {
            outcome = "failure";
            throw ex;
        } finally {
            sample.stop(timer(api, endpoint, outcome));
        }
    }

    private Timer timer(String api, String endpoint, String outcome) {
        return Timer.builder("external.api.calls")
            .tag("api", api)
            .tag("endpoint", endpoint)
            .tag("outcome", outcome)
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);
    }
}
