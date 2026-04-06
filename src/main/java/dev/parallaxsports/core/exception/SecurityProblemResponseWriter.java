package dev.parallaxsports.core.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityProblemResponseWriter {

    private final ObjectMapper objectMapper;

    public void writeUnauthorized(HttpServletRequest request, HttpServletResponse response, String detail) throws IOException {
        writeProblem(
            request,
            response,
            HttpStatus.UNAUTHORIZED,
            "/problems/unauthorized",
            "Unauthorized",
            detail
        );
    }

    public void writeForbidden(HttpServletRequest request, HttpServletResponse response, String detail) throws IOException {
        writeProblem(
            request,
            response,
            HttpStatus.FORBIDDEN,
            "/problems/forbidden",
            "Forbidden",
            detail
        );
    }

    private void writeProblem(
        HttpServletRequest request,
        HttpServletResponse response,
        HttpStatus status,
        String type,
        String title,
        String detail
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        Map<String, Object> problem = new LinkedHashMap<>();
        problem.put("type", type);
        problem.put("title", title);
        problem.put("status", status.value());
        problem.put("detail", detail);
        problem.put("instance", request.getRequestURI());

        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
