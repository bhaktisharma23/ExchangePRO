package com.exchangepro.common.error;

import com.exchangepro.common.web.CorrelationIdFilter;
import java.net.URI;
import java.time.Instant;
import org.slf4j.MDC;
import org.springframework.http.ProblemDetail;

/**
 * Single factory for the platform's error body, used both by the MVC {@link GlobalExceptionHandler} and by the
 * Spring Security filters (which run before MVC and therefore cannot use {@code @RestControllerAdvice}).
 */
public final class ApiProblems {

    private ApiProblems() {}

    public static ProblemDetail create(ErrorCode code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(code.status(), detail);
        problem.setType(URI.create("about:blank"));
        problem.setProperty("code", code.name());
        problem.setProperty("timestamp", Instant.now().toString());
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (correlationId != null) {
            problem.setProperty("correlationId", correlationId);
        }
        return problem;
    }
}
