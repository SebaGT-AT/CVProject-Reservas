package cl.reservas.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private static final Duration WINDOW = Duration.ofMinutes(5);
    private final Map<String, Window> attempts = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public AuthRateLimitFilter(ObjectMapper objectMapper) { this.objectMapper = objectMapper; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !request.getRequestURI().startsWith("/api/v1/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        int limit = path.endsWith("/login") ? 10
                : path.endsWith("/password/forgot") || path.endsWith("/verify-email/resend") ? 5 : 30;
        String ip = request.getRemoteAddr();
        String key = ip + ":" + path;
        Window window = attempts.compute(key, (ignored, current) ->
                current == null || current.startedAt.plus(WINDOW).isBefore(Instant.now())
                        ? new Window(Instant.now(), 1)
                        : new Window(current.startedAt, current.count + 1));
        if (window.count > limit) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(WINDOW.toSeconds()));
            objectMapper.writeValue(response.getOutputStream(), Map.of(
                    "type", "https://reservas.local/problems/rate-limit",
                    "title", "Demasiadas solicitudes",
                    "status", 429,
                    "detail", "Espera unos minutos antes de intentarlo nuevamente"));
            return;
        }
        chain.doFilter(request, response);
    }

    private record Window(Instant startedAt, int count) {}
}
