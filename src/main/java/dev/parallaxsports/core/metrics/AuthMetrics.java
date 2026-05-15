package dev.parallaxsports.core.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthMetrics {

    private final MeterRegistry registry;

    public void loginSuccess() {
        counter("auth.login.total", "outcome", "success", "reason", "ok").increment();
    }

    public void loginFailure(String reason) {
        counter("auth.login.total", "outcome", "failure", "reason", reason).increment();
    }

    public void register() {
        counter("auth.register.total").increment();
    }

    public void refresh(String outcome) {
        counter("auth.refresh.total", "outcome", outcome).increment();
    }

    public void logout() {
        counter("auth.logout.total").increment();
    }

    public void oauthLogin(String provider) {
        counter("auth.oauth.login.total", "provider", provider).increment();
    }

    public void jwtValidationFailure(String reason) {
        counter("auth.jwt.validation.failures.total", "reason", reason).increment();
    }

    private Counter counter(String name, String... tags) {
        return Counter.builder(name).tags(tags).register(registry);
    }
}
