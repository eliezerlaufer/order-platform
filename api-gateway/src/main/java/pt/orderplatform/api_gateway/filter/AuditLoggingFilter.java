package pt.orderplatform.api_gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Base64;

// =============================================================================
// AUDIT LOGGING FILTER — registo de todos os pedidos HTTP
// =============================================================================
// Emite uma linha JSON por pedido com:
//   type      — "audit" (permite filtrar em Grafana/Loki)
//   method    — GET | POST | PUT | DELETE | PATCH
//   path      — URI do pedido (sem query string)
//   ip        — IP do cliente (respeita X-Forwarded-For do load balancer)
//   userId    — subject do JWT (ou "anonymous" se sem token / inválido)
//   status    — código HTTP da resposta
//   durationMs— tempo total de processamento em milissegundos
//
// O userId é extraído manualmente do JWT (decode Base64 do payload).
// Não valida a assinatura — apenas lê o campo "sub" para logging.
//
// @Order(2) — executa depois do RateLimitFilter (Order 1).
// Pedidos rejeitados por rate limit (429) também são registados.
// =============================================================================
@Slf4j
@Component
@Order(2)
public class AuditLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String ip = resolveClientIp(request);
        String method = request.getMethod();
        String path = request.getRequestURI();
        String userId = extractUserId(request);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            int status = response.getStatus();
            log.info("{\"type\":\"audit\",\"method\":\"{}\",\"path\":\"{}\",\"ip\":\"{}\",\"userId\":\"{}\",\"status\":{},\"durationMs\":{}}",
                    method, path, ip, userId, status, durationMs);
        }
    }

    // =========================================================================
    // Extrai o "sub" claim do JWT sem validar assinatura (apenas para logging)
    // =========================================================================
    private String extractUserId(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return "anonymous";
        }
        try {
            String[] parts = auth.substring(7).split("\\.");
            if (parts.length < 2) return "anonymous";
            String json = new String(Base64.getUrlDecoder().decode(parts[1]));
            int start = json.indexOf("\"sub\":\"");
            if (start == -1) return "anonymous";
            start += 7;
            int end = json.indexOf('"', start);
            return end > start ? json.substring(start, end) : "anonymous";
        } catch (Exception e) {
            return "anonymous";
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
