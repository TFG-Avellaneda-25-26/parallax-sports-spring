package dev.parallaxsports.audit.repository;

import dev.parallaxsports.audit.model.AuditLog;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByUser_IdAndCreatedAtBetween(Long userId, OffsetDateTime from, OffsetDateTime to);

    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Long entityId);
}
