package dev.parallaxsports.audit.service;

import dev.parallaxsports.audit.model.AuditLog;
import dev.parallaxsports.audit.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async("auditExecutor")
    public void record(
        String action,
        Long actorUserId,
        String entityType,
        Long entityId,
        Map<String, Object> detail
    ) {
        try {
            AuditLog entry = AuditLog.builder()
                .action(action)
                .actorUserId(actorUserId)
                .entityType(entityType)
                .entityId(entityId)
                .detail(detail)
                .source(currentSource())
                .ipAddress(currentIp())
                .traceId(MDC.get("traceId"))
                .build();
            auditLogRepository.save(entry);
        } catch (Exception ex) {
            log.warn("Failed to write audit_log action='{}' actor={}: {}", action, actorUserId, ex.getMessage());
        }
    }

    private String currentIp() {
        HttpServletRequest req = currentRequest();
        if (req == null) {
            return null;
        }
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    private String currentSource() {
        HttpServletRequest req = currentRequest();
        return req != null ? req.getMethod() + " " + req.getRequestURI() : "system";
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes sra) {
            return sra.getRequest();
        }
        return null;
    }
}
