package pt.orderplatform.api_gateway.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

// =============================================================================
// RATE LIMIT FILTER — 60 pedidos por minuto por IP
// =============================================================================
// Usa Bucket4j (token bucket algorithm):
//   - Capacidade: 60 tokens
//   - Recarga: 60 tokens/minuto (= 1 token/segundo)
//   - Um pedido consome 1 token; se não houver tokens → 429 Too Many Requests
//
// ConcurrentHashMap<IP, Bucket> — sem Redis, sem dependências externas.
// Adequado para uma única instância do gateway (ambiente local/dev).
//
// Para produção multi-instância: usar bucket4j-redis para estado partilhado.
//
// @Order(1) — executa antes do AuditLoggingFilter (Order 2) e do gateway routing.
// =============================================================================
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String ip = resolveClientIp(request);
        Bucket bucket = buckets.computeIfAbsent(ip, this::newBucket);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"status\":429,\"error\":\"Too Many Requests\"," +
                    "\"message\":\"Rate limit exceeded: 60 requests per minute per IP\"}"
            );
        }
    }

    private Bucket newBucket(String ip) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(60)
                .refillIntervally(60, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
