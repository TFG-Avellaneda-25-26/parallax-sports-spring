package dev.parallaxsports.core.exception;

import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /*============================================================
      DOMAIN EXCEPTIONS
      Business-level errors related to resource state and rules
    ============================================================*/

    // -> Triggers: requested resource does not exist || Returns: Not Found (404)
    /**
     * Handles missing domain resources.
     *
     * @param ex domain exception with client-safe detail message
     * @param request current web request
     * @return RFC ProblemDetail payload with status 404
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex, WebRequest request) {
        logHandled(HttpStatus.NOT_FOUND, ex, request);
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    // -> Triggers: business rule validation fails || Returns: Bad Request (400)
    /**
     * Handles domain-specific bad request scenarios.
     *
     * @param ex domain exception with client-safe detail message
     * @param request current web request
     * @return RFC ProblemDetail payload with status 400
     */
    @ExceptionHandler(BadRequestException.class)
    public ProblemDetail handleBadRequest(BadRequestException ex, WebRequest request) {
        logHandled(HttpStatus.BAD_REQUEST, ex, request);
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    // -> Triggers: app-level unauthorized flow || Returns: Unauthorized (401)
    /**
     * Handles explicit unauthorized exceptions thrown by application logic.
     *
     * @param ex unauthorized exception
     * @param request current web request
     * @return RFC ProblemDetail payload with status 401
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ProblemDetail handleUnauthorized(UnauthorizedException ex, WebRequest request) {
        logHandled(HttpStatus.UNAUTHORIZED, ex, request);
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    // -> Triggers: duplicate business resource || Returns: Conflict (409)
    /**
     * Handles duplicate-resource cases (for example, repeated unique fields).
     *
     * @param ex duplicate exception
     * @param request current web request
     * @return RFC ProblemDetail payload with status 409
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicate(DuplicateResourceException ex, WebRequest request) {
        logHandled(HttpStatus.CONFLICT, ex, request);
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    /*============================================================
      PERSISTENCE EXCEPTIONS
      DB-integrity and relational-constraint translation
    ============================================================*/

    // -> Triggers: DB integrity violation || Returns: Conflict (409) or Bad Request (400)
    /**
     * Translates low-level data integrity violations to stable API responses.
     *
     * @param ex data integrity exception from persistence layer
     * @param request current web request
     * @return RFC ProblemDetail payload with status 409 or 400
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
        // DB exception need to unwrap it to get info -> extractMostSpecificMessage 
        // goes inside the wrapper and filters the error in DB to the make it presentable
        String reason = extractMostSpecificMessage(ex).toLowerCase();
        if (reason.contains("unique") || reason.contains("duplicate")) {
            logHandled(HttpStatus.CONFLICT, ex, request);
            return build(HttpStatus.CONFLICT, "Resource conflicts with an existing record", request);
        }
        if (reason.contains("check constraint") || reason.contains("foreign key")) {
            logHandled(HttpStatus.BAD_REQUEST, ex, request);
            return build(HttpStatus.BAD_REQUEST, "Request violates data constraints", request);
        }
        logHandled(HttpStatus.BAD_REQUEST, ex, request);
        return build(HttpStatus.BAD_REQUEST, "Invalid data", request);
    }

        /*============================================================
            SECURITY EXCEPTIONS
            Authentication and authorization boundaries
        ============================================================*/

        // -> Triggers: authenticated user lacks permission || Returns: Forbidden (403)
        /**
         * Handles authorization denials for protected resources.
         *
         * @param ex access denied exception
         * @param request current web request
         * @return RFC ProblemDetail payload with status 403
         */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        logHandled(HttpStatus.FORBIDDEN, ex, request);
        return build(HttpStatus.FORBIDDEN, "Access denied", request);
    }

        // -> Triggers: Spring Security auth failure (includes BadCredentialsException) || Returns: Unauthorized (401)
        /**
         * Handles Spring Security authentication failures.
         *
         * @param ex authentication exception (includes bad credentials and similar auth errors)
         * @param request current web request
         * @return RFC ProblemDetail payload with status 401
         */
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationFailure(AuthenticationException ex, WebRequest request) {
        logHandled(HttpStatus.UNAUTHORIZED, ex, request);
        return build(HttpStatus.UNAUTHORIZED, "Invalid credentials", request);
    }

        /*============================================================
            VALIDATION EXCEPTIONS
            Bean validation and request body parsing failures
        ============================================================*/

        // -> Triggers: method parameter constraint violations || Returns: Bad Request (400)
        /**
         * Handles method-level constraint validation errors.
         *
         * @param ex constraint violation exception
         * @param request current web request
         * @return RFC ProblemDetail payload with status 400 and violation map
         */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        logHandled(HttpStatus.BAD_REQUEST, ex, request);
        ProblemDetail problemDetail = build(HttpStatus.BAD_REQUEST, "Validation failed", request);
        // in bean validation -> property path is what constraint failed
        Map<String, String> violations = ex.getConstraintViolations().stream().collect(Collectors.toMap(
            violation -> violation.getPropertyPath().toString(),
            violation -> violation.getMessage() == null ? "Invalid value" : violation.getMessage(),
            (first, second) -> first
        ));
        problemDetail.setProperty("errors", violations);
        return problemDetail;
    }

    // -> Triggers: @Valid request-body field errors || Returns: Bad Request (400)
    /**
     * Handles bean validation failures for request body DTOs.
     *
     * @param ex method argument validation exception
     * @param request current web request
     * @return RFC ProblemDetail payload with status 400 and field error map
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException ex, WebRequest request) {
        logHandled(HttpStatus.BAD_REQUEST, ex, request);
        ProblemDetail problemDetail = build(HttpStatus.BAD_REQUEST, "Validation failed", request);
        Map<String, String> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                error -> error.getField(),
                error -> error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage(),
                (first, second) -> first
            ));
        problemDetail.setProperty("errors", fieldErrors);
        return problemDetail;
    }

        // -> Triggers: malformed JSON / unreadable request body || Returns: Bad Request (400)
        /**
         * Handles JSON parse and request-body deserialization failures.
         *
         * @param ex unreadable message exception
         * @param request current web request
         * @return RFC ProblemDetail payload with status 400
         */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleHttpMessageNotReadable(HttpMessageNotReadableException ex, WebRequest request) {
        logHandled(HttpStatus.BAD_REQUEST, ex, request);
        return build(HttpStatus.BAD_REQUEST, "Malformed request body", request);
    }

        /*============================================================
            SAFETY NET
            Last-resort fallback for uncaught exceptions
        ============================================================*/

        // -> Triggers: unhandled exception fallback || Returns: Internal Server Error (500)
        /**
         * Handles uncaught exceptions not matched by more specific handlers.
         *
         * @param ex unexpected exception
         * @param request current web request
         * @return RFC ProblemDetail payload with status 500
         */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, WebRequest request) {
        logHandled(HttpStatus.INTERNAL_SERVER_ERROR, ex, request);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
    }

        /*============================================================
            INTERNAL HELPERS
            Logging, payload shaping, and DB message extraction
        ============================================================*/

        /**
         * Writes structured exception logs for observability pipelines.
         *
         * @param status HTTP status that will be returned
         * @param ex exception to log
         * @param request current web request
         */
    private void logHandled(HttpStatus status, Throwable ex, WebRequest request) {
        String path = requestPath(request);
        String exceptionType = ex == null ? "UnknownException" : ex.getClass().getSimpleName();
        String message = ex == null || ex.getMessage() == null ? "" : ex.getMessage();

        if (status.is5xxServerError()) {
            log.error(
                "api_exception status={} path='{}' exception='{}' message='{}'",
                status.value(),
                path,
                exceptionType,
                message,
                ex
            );
            return;
        }

        log.warn(
            "api_exception status={} path='{}' exception='{}' message='{}'",
            status.value(),
            path,
            exceptionType,
            message
        );
    }

    /**
     * Builds a ProblemDetail payload with status, title, detail, and instance path.
     *
     * @param status HTTP status for the response
     * @param message client-safe detail message
     * @param request current web request
     * @return initialized ProblemDetail object
     */
    private ProblemDetail build(HttpStatus status, String message, WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, message);
        problemDetail.setTitle(status.getReasonPhrase());
        String path = requestPath(request);
        if (path != null && !path.isBlank()) {
            problemDetail.setInstance(URI.create(path));
        }
        return problemDetail;
    }

    /**
     * Extracts request path for logs and ProblemDetail instance field.
     *
     * @param request current web request
     * @return request URI or "unknown" when not running in servlet context
     */
    private String requestPath(WebRequest request) {
        return request instanceof ServletWebRequest servletWebRequest
            ? servletWebRequest.getRequest().getRequestURI()
            : "unknown";
    }

    /**
     * Extracts the deepest available database error message.
     *
     * @param ex data integrity violation exception
     * @return most specific cause message, or fallback exception message, or empty string
     */
    private String extractMostSpecificMessage(DataIntegrityViolationException ex) {
        Throwable mostSpecificCause = ex.getMostSpecificCause();
        if (mostSpecificCause != null && mostSpecificCause.getMessage() != null) {
            return mostSpecificCause.getMessage();
        }
        return ex.getMessage() == null ? "" : ex.getMessage();
    }
}
