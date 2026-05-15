package dev.parallaxsports.auth.service;

import dev.parallaxsports.user.repository.UserRepository;
import io.micrometer.core.annotation.Timed;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UnverifiedUserCleanupScheduler {

	private static final int EXPIRY_DAYS = 14;

	private final UserRepository userRepository;

	@Scheduled(cron = "0 0 3 * * *")
	@Transactional
	@Timed(value = "scheduled_job_seconds", extraTags = {"job", "unverified-user-cleanup"}, histogram = true, percentiles = {0.5, 0.95, 0.99})
	public void cleanupUnverifiedUsers() {
		OffsetDateTime cutoff = OffsetDateTime.now().minusDays(EXPIRY_DAYS);
		int deleted = userRepository.deleteUnverifiedUsersBefore(cutoff);
		if (deleted > 0) {
			log.info("Deleted {} unverified users registered before {}", deleted, cutoff);
		}
	}
}
