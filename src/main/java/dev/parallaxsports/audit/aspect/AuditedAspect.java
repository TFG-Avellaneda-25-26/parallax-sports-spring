package dev.parallaxsports.audit.aspect;

import dev.parallaxsports.audit.annotation.Audited;
import dev.parallaxsports.audit.service.AuditService;
import dev.parallaxsports.user.model.User;
import dev.parallaxsports.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditedAspect {

    private final AuditService auditService;
    private final UserRepository userRepository;

    @Around("@annotation(dev.parallaxsports.audit.annotation.Audited)")
    public Object audit(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        Audited annotation = method.getAnnotation(Audited.class);

        Long actorId = resolveActorId();
        String entityType = annotation.entityType().isEmpty() ? null : annotation.entityType();
        Map<String, Object> detail = annotation.includeArgs() ? extractArgs(signature, pjp.getArgs()) : null;

        try {
            Object result = pjp.proceed();
            auditService.record(annotation.action(), actorId, entityType, null, detail);
            return result;
        } catch (Throwable ex) {
            Map<String, Object> failDetail = detail == null ? new HashMap<>() : new HashMap<>(detail);
            failDetail.put("error", ex.getClass().getSimpleName());
            failDetail.put("errorMessage", ex.getMessage());
            auditService.record(annotation.action() + "_FAILED", actorId, entityType, null, failDetail);
            throw ex;
        }
    }

    private Long resolveActorId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserDetails details)) {
            return null;
        }
        return userRepository.findByEmail(details.getUsername()).map(User::getId).orElse(null);
    }

    private Map<String, Object> extractArgs(MethodSignature signature, Object[] args) {
        Map<String, Object> map = new LinkedHashMap<>();
        String[] names = signature.getParameterNames();
        Class<?>[] types = signature.getParameterTypes();
        for (int i = 0; i < args.length; i++) {
            String name = names != null && i < names.length ? names[i] : "arg" + i;
            if (isSensitiveName(name) || isSensitiveType(types[i])) {
                continue;
            }
            map.put(name, summarize(args[i]));
        }
        return map.isEmpty() ? null : map;
    }

    private boolean isSensitiveName(String name) {
        String n = name.toLowerCase();
        return n.contains("password") || n.contains("secret") || n.contains("token");
    }

    private boolean isSensitiveType(Class<?> type) {
        return HttpServletRequest.class.isAssignableFrom(type)
            || HttpServletResponse.class.isAssignableFrom(type)
            || MultipartFile.class.isAssignableFrom(type);
    }

    private Object summarize(Object value) {
        if (value == null) return null;
        if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean) return value;
        return value.toString();
    }
}
