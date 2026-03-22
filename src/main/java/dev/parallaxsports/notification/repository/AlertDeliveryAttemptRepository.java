package dev.parallaxsports.notification.repository;

import dev.parallaxsports.notification.model.AlertDeliveryAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for immutable delivery attempt records.
 */
public interface AlertDeliveryAttemptRepository extends JpaRepository<AlertDeliveryAttempt, Long> {

	/**
	 * Checks whether an attempt with the same alert/message/outcome tuple already exists.
	 *
	 * @param alertId alert identifier
	 * @param streamMessageId Redis stream message id associated with the callback
	 * @param outcome normalized attempt outcome category
	 * @return true when a duplicate attempt should be ignored
	 */
	boolean existsByAlert_IdAndStreamMessageIdAndOutcome(Long alertId, String streamMessageId, String outcome);
}
