package dev.parallaxsports.notification.repository;

import dev.parallaxsports.notification.model.AlertArtifact;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for alert artifact persistence.
 *
 * Provides CRUD access to generated media asset metadata that can be attached
 * to artifact-gated alert deliveries.
 */
public interface AlertArtifactRepository extends JpaRepository<AlertArtifact, Long> {
}
