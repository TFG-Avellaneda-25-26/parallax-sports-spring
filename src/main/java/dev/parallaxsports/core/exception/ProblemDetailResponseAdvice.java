package dev.parallaxsports.core.exception;

import java.net.URI;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 *  Before serialization -> builds the Problem Details responses
 *
 * Spring framework and Spring Security can produce ProblemDetail responses
 * that never pass through {@link GlobalExceptionHandler}
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
    public class ProblemDetailResponseAdvice implements ResponseBodyAdvice<Object> {

    private static final String BASE = "/problems";

    @Override
    public boolean supports(
        MethodParameter returnType,
        Class<? extends HttpMessageConverter<?>> converterType
    ) {
        return true;
    }

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

    private String typeForStatus(Integer status) {
        if (status == null) {
            return BASE + "/internal-error";
        }
        return switch (status) {
            case 400 -> BASE + "/bad-request";
            case 401 -> BASE + "/unauthorized";
            case 403 -> BASE + "/forbidden";
            case 404 -> BASE + "/not-found";
            case 409 -> BASE + "/conflict";
            case 502 -> BASE + "/bad-gateway";
            case 503 -> BASE + "/service-unavailable";
            case 500 -> BASE + "/internal-error";
            default -> BASE + "/http-" + status;
        };
    }
}
