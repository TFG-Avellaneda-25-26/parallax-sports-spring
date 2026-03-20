package dev.parallaxsports.notification.repository;

import dev.parallaxsports.notification.model.AlertArtifact;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertArtifactRepository extends JpaRepository<AlertArtifact, Long> {
}
