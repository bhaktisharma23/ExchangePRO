package com.exchangepro.common.error;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Converts every exception thrown by a controller into a consistent RFC 9457 "Problem Details" JSON body:
 *
 * <pre>{@code
 * { "type": "about:blank", "title": "Bad Request", "status": 400, "detail": "...",
 *   "code": "VALIDATION_FAILED", "correlationId": "...", "timestamp": "...", "errors": [...] }
 * }</pre>
 *
 * <p>Internal details (stack traces, SQL, class names) are never sent to clients; they are logged with the
 * correlation id so a support engineer can find them from the id the user sees.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** A single field-level validation problem. */
    public record FieldViolation(String field, String message) {}

    @ExceptionHandler(ExchangeProException.class)
    ResponseEntity<ProblemDetail> handleDomain(ExchangeProException ex) {
        return build(ex.errorCode(), ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleBodyValidation(MethodArgumentNotValidException ex) {
        List<FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldViolation(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return build(ErrorCode.VALIDATION_FAILED, "Request validation failed", violations);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ProblemDetail> handleParamValidation(ConstraintViolationException ex) {
        List<FieldViolation> violations = ex.getConstraintViolations().stream()
                .map(v -> new FieldViolation(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return build(ErrorCode.VALIDATION_FAILED, "Request validation failed", violations);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ProblemDetail> handleMalformed(Exception ex) {
        return build(ErrorCode.MALFORMED_REQUEST, "Malformed request", null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ProblemDetail> handleNoResource(NoResourceFoundException ex) {
        return build(ErrorCode.RESOURCE_NOT_FOUND, "No endpoint " + ex.getResourcePath(), null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ProblemDetail> handleMethod(HttpRequestMethodNotSupportedException ex) {
        return build(ErrorCode.METHOD_NOT_ALLOWED, "Method " + ex.getMethod() + " is not supported", null);
    }

    /** Thrown by method-level security (e.g. {@code @PreAuthorize}) inside controllers/services. */
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
        return build(ErrorCode.FORBIDDEN, "You do not have permission to perform this action", null);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex) {
        return build(ErrorCode.UNAUTHORIZED, "Authentication is required", null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return build(ErrorCode.INTERNAL_ERROR, "An unexpected error occurred", null);
    }

    private ResponseEntity<ProblemDetail> build(ErrorCode code, String detail, List<FieldViolation> violations) {
        ProblemDetail problem = ApiProblems.create(code, detail);
        if (violations != null) {
            problem.setProperty("errors", violations);
        }
        return ResponseEntity.status(code.status()).body(problem);
    }
}
