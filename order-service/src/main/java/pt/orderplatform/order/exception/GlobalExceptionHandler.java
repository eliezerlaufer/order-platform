package pt.orderplatform.order.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

// =============================================================================
// GLOBAL EXCEPTION HANDLER
// =============================================================================
// @RestControllerAdvice → intercepta excepções lançadas em qualquer Controller
// e devolve respostas HTTP formatadas em vez de stack traces.
//
// Usamos ProblemDetail (RFC 7807) — o formato standard para erros HTTP em APIs.
// Spring Boot 3+ tem suporte nativo a ProblemDetail.
//
// Exemplo de resposta de erro:
// {
//   "type": "https://api.orderplatform.pt/errors/not-found",
//   "title": "Order Not Found",
//   "status": 404,
//   "detail": "Order not found: 550e8400-..."
// }
// =============================================================================
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // -------------------------------------------------------------------------
    // 404 — Pedido não encontrado
    // -------------------------------------------------------------------------
    @ExceptionHandler(OrderNotFoundException.class)
    public ProblemDetail handleOrderNotFound(OrderNotFoundException ex) {
        log.warn("Order not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Order Not Found");
        problem.setType(URI.create("https://api.orderplatform.pt/errors/not-found"));
        return problem;
    }

    // -------------------------------------------------------------------------
    // 422 — Cancelamento inválido (regra de negócio)
    // -------------------------------------------------------------------------
    @ExceptionHandler(OrderCancellationException.class)
    public ProblemDetail handleOrderCancellation(OrderCancellationException ex) {
        log.warn("Order cancellation failed: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Order Cancellation Not Allowed");
        problem.setType(URI.create("https://api.orderplatform.pt/errors/cancellation-not-allowed"));
        return problem;
    }

    // -------------------------------------------------------------------------
    // 400 — Validação falhou (@Valid no Controller)
    // -------------------------------------------------------------------------
    // MethodArgumentNotValidException é lançada pelo Spring quando @Valid falha.
    // Extraímos os erros de cada campo e devolvemos um mapa.
    // -------------------------------------------------------------------------
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        // Se o mesmo campo tiver dois erros, mantém o primeiro
                        (first, second) -> first
                ));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Request validation failed");
        problem.setTitle("Validation Error");
        problem.setType(URI.create("https://api.orderplatform.pt/errors/validation"));
        problem.setProperty("errors", fieldErrors);
        return problem;
    }

    // -------------------------------------------------------------------------
    // 500 — Erros inesperados (último recurso)
    // -------------------------------------------------------------------------
    // Importante: logar com ERROR e stack trace, mas devolver mensagem genérica
    // ao cliente. Nunca expor detalhes internos (stack traces, nomes de tabelas, etc.)
    // -------------------------------------------------------------------------
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("https://api.orderplatform.pt/errors/internal"));
        return problem;
    }
}
