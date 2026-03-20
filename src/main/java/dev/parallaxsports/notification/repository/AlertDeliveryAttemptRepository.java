package dev.parallaxsports.notification.repository;

import dev.parallaxsports.notification.model.AlertDeliveryAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertDeliveryAttemptRepository extends JpaRepository<AlertDeliveryAttempt, Long> {
}
