package dev.parallaxsports.audit.dto;

import dev.parallaxsports.audit.model.AuditLog;
import java.time.OffsetDateTime;
import java.util.Map;

public record AuditLogResponse(
    Long id,
    Long actorUserId,
    String action,
    String source,
    String entityType,
    Long entityId,
    Map<String, Object> detail,
    String ipAddress,
    String traceId,
    OffsetDateTime createdAt
) {
    public static AuditLogResponse from(AuditLog entity) {
        return new AuditLogResponse(
            entity.getId(),
            entity.getActorUserId(),
            entity.getAction(),
            entity.getSource(),
            entity.getEntityType(),
            entity.getEntityId(),
            entity.getDetail(),
            entity.getIpAddress(),
            entity.getTraceId(),
            entity.getCreatedAt()
        );
    }
}
