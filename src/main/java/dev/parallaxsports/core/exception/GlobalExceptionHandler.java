package dev.parallaxsports.core.exception;

import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;


@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String BASE = "/problems";
    private static final String typeNotFound = BASE + "/not-found";
    private static final String typeBadRequest = BASE + "/bad-request";
    private static final String typeUnauthorized = BASE + "/unauthorized";
    private static final String typeForbidden = BASE + "/forbidden";
    private static final String typeConflict = BASE + "/conflict";
    private static final String typeValidation = BASE + "/validation-error";
    private static final String typeMalformedBody = BASE + "/malformed-body";
    private static final String typeBadGateway = BASE + "/bad-gateway";
    private static final String typeServiceUnavailable = BASE + "/service-unavailable";
    private static final String typeConfigurationError = BASE + "/configuration-error";
    private static final String typeInternal = BASE + "/internal-error";
a


    // -> Triggers: @Valid request-body field errors || Returns: Bad Request (400) + invalid_params
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        logHandled(HttpStatus.BAD_REQUEST, ex, request);
        ProblemDetail problem = buildProblem(
            typeValidation,
            HttpStatus.BAD_REQUEST,
            "Validation Error",
            "Your request parameters did not validate.",
            request
        );
        List<Map<String, String>> invalidParams = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> Map.of(
                "name", error.getField(),
                "reason", error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage()
            ))
            .toList();
        problem.setProperty("invalid_params", invalidParams);
        return ResponseEntity.badRequest().body(problem);
    }

    // -> Triggers: malformed JSON / unreadable request body || Returns: Bad Request (400)
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
        HttpMessageNotReadableException ex,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        logHandled(HttpStatus.BAD_REQUEST, ex, request);
        return ResponseEntity.badRequest().body(
            buildProblem(typeMalformedBody, HttpStatus.BAD_REQUEST, "Malformed Request Body", "Malformed request body", request)
        );
    }

    /*============================================================
      DOMAIN EXCEPTIONS
      Business-level errors related to resource state and rules
    ============================================================*/

    // -> Triggers: requested resource does not exist || Returns: Not Found (404)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex, WebRequest request) {
        logHandled(HttpStatus.NOT_FOUND, ex, request);
        return buildProblem(typeNotFound, HttpStatus.NOT_FOUND, "Resource Not Found", ex.getMessage(), request);
    }

    // -> Triggers: business rule validation fails || Returns: Bad Request (400)
    @ExceptionHandler(BadRequestException.class)
    public ProblemDetail handleBadRequest(BadRequestException ex, WebRequest request) {
        logHandled(HttpStatus.BAD_REQUEST, ex, request);
        return buildProblem(typeBadRequest, HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
    }

    // -> Triggers: app-level unauthorized flow || Returns: Unauthorized (401)
    @ExceptionHandler(UnauthorizedException.class)
    public ProblemDetail handleUnauthorized(UnauthorizedException ex, WebRequest request) {
        logHandled(HttpStatus.UNAUTHORIZED, ex, request);
        return buildProblem(typeUnauthorized, HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage(), request);
    }

    // -> Triggers: duplicate business resource || Returns: Conflict (409)
    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicate(DuplicateResourceException ex, WebRequest request) {
        logHandled(HttpStatus.CONFLICT, ex, request);
        return buildProblem(typeConflict, HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request);
    }

    // -> Triggers: invalid state transition / lifecycle conflict || Returns: Conflict (409)
    @ExceptionHandler(StateConflictException.class)
    public ProblemDetail handleStateConflict(StateConflictException ex, WebRequest request) {
        logHandled(HttpStatus.CONFLICT, ex, request);
        return buildProblem(typeConflict, HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request);
    }

    /*============================================================
      PERSISTENCE EXCEPTIONS
      DB-integrity and relational-constraint translation
    ============================================================*/

    // -> Triggers: DB integrity violation || Returns: Conflict (409) or Bad Request (400)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
        String reason = extractMostSpecificMessage(ex).toLowerCase();
        if (reason.contains("unique") || reason.contains("duplicate")) {
            logHandled(HttpStatus.CONFLICT, ex, request);
            return buildProblem(typeConflict, HttpStatus.CONFLICT, "Conflict", "Resource conflicts with an existing record", request);
        }
        if (reason.contains("check constraint") || reason.contains("foreign key")) {
            logHandled(HttpStatus.BAD_REQUEST, ex, request);
            return buildProblem(typeBadRequest, HttpStatus.BAD_REQUEST, "Bad Request", "Request violates data constraints", request);
        }
        logHandled(HttpStatus.BAD_REQUEST, ex, request);
        return buildProblem(typeBadRequest, HttpStatus.BAD_REQUEST, "Bad Request", "Invalid data", request);
    }

    /*============================================================
      SECURITY EXCEPTIONS
      Authentication and authorization boundaries
    ============================================================*/

    // -> Triggers: authenticated user lacks permission (e.g. @RequiresVerifiedEmail AOP) || Returns: Forbidden (403)
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        logHandled(HttpStatus.FORBIDDEN, ex, request);
        return buildProblem(typeForbidden, HttpStatus.FORBIDDEN, "Forbidden", "Access denied", request);
    }

    // -> Triggers: Spring Security auth failure thrown from application code || Returns: Unauthorized (401)
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationFailure(AuthenticationException ex, WebRequest request) {
        logHandled(HttpStatus.UNAUTHORIZED, ex, request);
        return buildProblem(typeUnauthorized, HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid credentials", request);
    }

    /*============================================================
      VALIDATION EXCEPTIONS
      Bean validation constraint failures
    ============================================================*/

    // -> Triggers: method parameter constraint violations || Returns: Bad Request (400) + invalid_params
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        logHandled(HttpStatus.BAD_REQUEST, ex, request);
        ProblemDetail problem = buildProblem(
            typeValidation,
            HttpStatus.BAD_REQUEST,
            "Validation Error",
            "Your request parameters did not validate.",
            request
        );
        List<Map<String, String>> invalidParams = ex.getConstraintViolations().stream()
            .map(violation -> Map.of(
                "name", violation.getPropertyPath().toString(),
                "reason", violation.getMessage() == null ? "Invalid value" : violation.getMessage()
            ))
            .toList();
        problem.setProperty("invalid_params", invalidParams);
        return problem;
    }

    /*============================================================
      INTEGRATION AND INFRASTRUCTURE EXCEPTIONS
      Upstream failures, transport errors, and system availability issues
    ============================================================*/

    // -> Triggers: explicit status raised by lower application layers || Returns: propagated status
    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatus(ResponseStatusException ex, WebRequest request) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        logHandled(status, ex, request);
        String detail = ex.getReason() == null ? status.getReasonPhrase() : ex.getReason();
        return buildProblem("about:blank", status, status.getReasonPhrase(), detail, request);
    }

    // -> Triggers: upstream dependency returned an error response || Returns: Bad Gateway (502)
    @ExceptionHandler(UpstreamServiceException.class)
    public ProblemDetail handleUpstreamService(UpstreamServiceException ex, WebRequest request) {
        logHandled(HttpStatus.BAD_GATEWAY, ex, request);
        return buildProblem(typeBadGateway, HttpStatus.BAD_GATEWAY, "Bad Gateway", ex.getMessage(), request);
    }

    // -> Triggers: Redis or dependent service temporarily unavailable || Returns: Service Unavailable (503)
    @ExceptionHandler({
        ServiceUnavailableException.class,
        RedisConnectionFailureException.class,
        RedisSystemException.class
    })
    public ProblemDetail handleServiceUnavailable(RuntimeException ex, WebRequest request) {
        logHandled(HttpStatus.SERVICE_UNAVAILABLE, ex, request);
        return buildProblem(
            typeServiceUnavailable,
            HttpStatus.SERVICE_UNAVAILABLE,
            "Service Unavailable",
            ex.getMessage() == null ? "Service temporarily unavailable" : ex.getMessage(),
            request
        );
    }

    // -> Triggers: outbound HTTP client call failed || Returns: Bad Gateway (502)
    @ExceptionHandler(RestClientException.class)
    public ProblemDetail handleRestClientException(RestClientException ex, WebRequest request) {
        logHandled(HttpStatus.BAD_GATEWAY, ex, request);
        return buildProblem(typeBadGateway, HttpStatus.BAD_GATEWAY, "Bad Gateway", "Upstream service request failed", request);
    }

    // -> Triggers: missing/invalid runtime configuration || Returns: Internal Server Error (500) or Service Unavailable (503)
    @ExceptionHandler(SystemConfigurationException.class)
    public ProblemDetail handleSystemConfiguration(SystemConfigurationException ex, WebRequest request) {
        String message = ex.getMessage() == null ? "System configuration error" : ex.getMessage();
        boolean missingRequiredConfig = message.toLowerCase().contains("must be configured");
        if (missingRequiredConfig) {
            logHandled(HttpStatus.SERVICE_UNAVAILABLE, ex, request);
            return buildProblem(typeConfigurationError, HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", message, request);
        }
        logHandled(HttpStatus.INTERNAL_SERVER_ERROR, ex, request);
        return buildProblem(typeInternal, HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", message, request);
    }

    /*============================================================
      SAFETY NET
      Last-resort fallback for uncaught exceptions
    ============================================================*/

    // -> Triggers: unhandled exception fallback || Returns: Internal Server Error (500)
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, WebRequest request) {
        logHandled(HttpStatus.INTERNAL_SERVER_ERROR, ex, request);
        return buildProblem(typeInternal, HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred", request);
    }

    /*============================================================
      INTERNAL HELPERS
      Logging, payload shaping, and DB message extraction
    ============================================================*/

    private void logHandled(HttpStatus status, Throwable ex, WebRequest request) {
        String path = requestPath(request);
        String exceptionType = ex == null ? "UnknownException" : ex.getClass().getSimpleName();
        String message = ex == null || ex.getMessage() == null ? "" : ex.getMessage();
        if (status.is5xxServerError()) {
            log.error("api_exception status={} path='{}' exception='{}' message='{}'",
                status.value(), path, exceptionType, message, ex);
            return;
        }
        log.warn("api_exception status={} path='{}' exception='{}' message='{}'",
            status.value(), path, exceptionType, message);
    }

    private ProblemDetail buildProblem(String type, HttpStatus status, String title, String detail, WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setType(URI.create(type));
        problemDetail.setTitle(title);
        problemDetail.setStatus(status.value());
        String path = requestPath(request);
        if (path != null && !path.isBlank() && !"unknown".equals(path)) {
            problemDetail.setInstance(URI.create(path));
        }
        return problemDetail;
    }

    private String requestPath(WebRequest request) {
        return request instanceof ServletWebRequest servletWebRequest
            ? servletWebRequest.getRequest().getRequestURI()
            : "unknown";
    }

    private String extractMostSpecificMessage(DataIntegrityViolationException ex) {
        Throwable mostSpecificCause = ex.getMostSpecificCause();
        if (mostSpecificCause != null && mostSpecificCause.getMessage() != null) {
            return mostSpecificCause.getMessage();
        }
        return ex.getMessage() == null ? "" : ex.getMessage();
    }
}
