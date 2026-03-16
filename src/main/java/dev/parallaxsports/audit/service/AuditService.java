package dev.parallaxsports.audit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.parallaxsports.audit.model.AuditLog;
import dev.parallaxsports.audit.repository.AuditLogRepository;
import dev.parallaxsports.user.model.User;
import java.time.OffsetDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditLog log(
        String action,
        String entityType,
        Long entityId,
        JsonNode detail,
        String ipAddress,
        User user
    ) {
        String traceId = MDC.get("traceId");
        AuditLog log = AuditLog.builder()
            .action(action)
            .entityType(entityType)
            .entityId(entityId)
            .detail(enrichDetail(detail))
            .ipAddress(ipAddress)
            .traceId(traceId)
            .user(user)
            .createdAt(OffsetDateTime.now())
            .build();
        return auditLogRepository.save(log);
    }

    public AuditLog log(
        String action,
        String entityType,
        Long entityId,
        JsonNode detail,
        String ipAddress
    ) {
        return log(action, entityType, entityId, detail, ipAddress, null);
    }

    private JsonNode enrichDetail(JsonNode detail) {
        ObjectNode payload = detail == null || !detail.isObject()
            ? objectMapper.createObjectNode()
            : (ObjectNode) detail.deepCopy();

        String traceId = MDC.get("traceId");
        if (traceId != null && !traceId.isBlank()) {
            payload.put("traceId", traceId);
        }

        return payload.isEmpty() ? null : payload;
    }

    public Optional<AuditLog> findById(Long id) {
        return auditLogRepository.findById(id);
    }
}
