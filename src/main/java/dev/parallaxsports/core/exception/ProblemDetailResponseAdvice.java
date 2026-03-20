package dev.parallaxsports.core.exception;

import java.net.URI;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Final ProblemDetail normalizer applied right before the response body is serialized.
 *
 * This advice exists to enforce a stable API contract for all error responses,
 * including framework-generated ProblemDetail payloads that may not pass through
 * {@link GlobalExceptionHandler}. It complements, but does not replace, exception
 * translation in {@link GlobalExceptionHandler}.
 *
 * Responsibility split:
 * - {@link GlobalExceptionHandler}: maps known exceptions to semantic status/title/detail/type.
 * - {@code ProblemDetailResponseAdvice}: guarantees missing fields are normalized consistently.
 */
@RestControllerAdvice
public class ProblemDetailResponseAdvice implements ResponseBodyAdvice<Object> {

    private static final String TYPE_NOT_FOUND = "http://localhost:4200/problems/not-found";
    private static final String TYPE_BAD_REQUEST = "http://localhost:4200/problems/bad-request";
    private static final String TYPE_UNAUTHORIZED = "http://localhost:4200/problems/unauthorized";
    private static final String TYPE_FORBIDDEN = "http://localhost:4200/problems/forbidden";
    private static final String TYPE_CONFLICT = "http://localhost:4200/problems/conflict";
    private static final String TYPE_BAD_GATEWAY = "http://localhost:4200/problems/bad-gateway";
    private static final String TYPE_SERVICE_UNAVAILABLE = "http://localhost:4200/problems/service-unavailable";
    private static final String TYPE_INTERNAL = "http://localhost:4200/problems/internal-error";

    /**
     * Enables this advice globally.
     *
        * Filtering is done inside
        * {@link #beforeBodyWrite(Object, MethodParameter, MediaType, Class, ServerHttpRequest, ServerHttpResponse)}
        * by checking if the response body is a {@link ProblemDetail}.
     */
    @Override
    public boolean supports(
        MethodParameter returnType,
        Class<? extends HttpMessageConverter<?>> converterType
    ) {
        return true;
    }

    /**
     * Normalizes outgoing ProblemDetail payloads after exception resolution and before JSON serialization.
     *
        * Rules:
        * - If {@code type} is missing or {@code about:blank}, set a deterministic type URI from status.
        * - If {@code instance} is missing, set it from the request path.
        * - Leave existing semantic values untouched when already provided.
     *
     * @param body response body returned by handler/advice
     * @param returnType selected controller method return type
     * @param selectedContentType selected response media type
     * @param selectedConverterType selected message converter
     * @param request current server request
     * @param response current server response
     * @return original body for non-ProblemDetail responses, or normalized ProblemDetail
     */
    @Override
    public Object beforeBodyWrite(
        Object body,
        MethodParameter returnType,
        MediaType selectedContentType,
        Class<? extends HttpMessageConverter<?>> selectedConverterType,
        ServerHttpRequest request,
        ServerHttpResponse response
    ) {
        if (!(body instanceof ProblemDetail problemDetail)) {
            return body;
        }

        URI type = problemDetail.getType();
        if (type == null || "about:blank".equals(type.toString())) {
            problemDetail.setType(URI.create(typeForStatus(problemDetail.getStatus())));
        }

        if (problemDetail.getInstance() == null && request != null && request.getURI() != null) {
            String path = request.getURI().getPath();
            if (path != null && !path.isBlank()) {
                problemDetail.setInstance(URI.create(path));
            }
        }

        return problemDetail;
    }

    /**
     * Maps HTTP status to default problem-type URI when no explicit semantic type exists.
     *
     * @param status HTTP status code from ProblemDetail
     * @return canonical type URI for known statuses, or a generic HTTP-status URI
     */
    private String typeForStatus(Integer status) {
        if (status == null) {
            return TYPE_INTERNAL;
        }

        return switch (status) {
            case 400 -> TYPE_BAD_REQUEST;
            case 401 -> TYPE_UNAUTHORIZED;
            case 403 -> TYPE_FORBIDDEN;
            case 404 -> TYPE_NOT_FOUND;
            case 409 -> TYPE_CONFLICT;
            case 502 -> TYPE_BAD_GATEWAY;
            case 503 -> TYPE_SERVICE_UNAVAILABLE;
            case 500 -> TYPE_INTERNAL;
            default -> "http://localhost:4200/problems/http-" + status;
        };
    }
}