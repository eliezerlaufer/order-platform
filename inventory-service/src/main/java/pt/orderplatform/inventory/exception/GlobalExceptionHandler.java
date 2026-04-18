package pt.orderplatform.inventory.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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
// GLOBAL EXCEPTION HANDLER — mapeia excepções de domínio para respostas HTTP
// =============================================================================
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 404 — produto inexistente
    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleProductNotFound(ProductNotFoundException ex) {
        log.warn("Product not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Product Not Found");
        problem.setType(URI.create("https://api.orderplatform.pt/errors/not-found"));
        return problem;
    }

    // 409 — stock insuficiente
    @ExceptionHandler(InsufficientStockException.class)
    public ProblemDetail handleInsufficientStock(InsufficientStockException ex) {
        log.warn("Insufficient stock: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Insufficient Stock");
        problem.setType(URI.create("https://api.orderplatform.pt/errors/insufficient-stock"));
        problem.setProperty("productId", ex.getProductId());
        problem.setProperty("requested", ex.getRequested());
        problem.setProperty("available", ex.getAvailable());
        return problem;
    }

    // 409 — SKU duplicado (violação de unique constraint)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "Resource conflict (possibly duplicate key)");
        problem.setTitle("Conflict");
        problem.setType(URI.create("https://api.orderplatform.pt/errors/conflict"));
        return problem;
    }

    // 400 — validação falhou
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (first, second) -> first
                ));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Request validation failed");
        problem.setTitle("Validation Error");
        problem.setType(URI.create("https://api.orderplatform.pt/errors/validation"));
        problem.setProperty("errors", fieldErrors);
        return problem;
    }

    // 400 — argumento inválido vindo do domínio
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Bad Request");
        problem.setType(URI.create("https://api.orderplatform.pt/errors/bad-request"));
        return problem;
    }

    // 500 — erro inesperado
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
