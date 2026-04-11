package dev.parallaxsports.bot.filter;

import dev.parallaxsports.core.config.properties.BotProperties;
import dev.parallaxsports.core.exception.SecurityProblemResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class BotApiKeyFilter extends OncePerRequestFilter {

    static final String HEADER = "X-Bot-Api-Key";
    private static final String BOT_PATH_PREFIX = "/api/bot/";

    private final BotProperties botProperties;
    private final SecurityProblemResponseWriter problemWriter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String incoming = request.getHeader(HEADER);

        byte[] expected = botProperties.getApiKey().getBytes(StandardCharsets.UTF_8);
        byte[] actual = (incoming != null ? incoming : "").getBytes(StandardCharsets.UTF_8);

        if (!MessageDigest.isEqual(expected, actual)) {
            problemWriter.writeUnauthorized(request, response, "Invalid or missing bot API key");
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(BOT_PATH_PREFIX);
    }
}
