package com.exchangepro.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gives every HTTP request a correlation id so all log lines produced while handling it can be grouped.
 *
 * <p>How it works:
 * <ol>
 *   <li>If the caller sent a well-formed {@code X-Correlation-Id} header, reuse it (lets a request be traced
 *       across services); otherwise generate a random UUID.</li>
 *   <li>Store it in the SLF4J {@link MDC} (a per-thread map). The log pattern prints {@code %X{correlationId}}.</li>
 *   <li>Echo it in the response header so the client (and the user reporting a bug) can quote it.</li>
 *   <li>Always remove it from the MDC afterwards: servlet threads are pooled and reused.</li>
 * </ol>
 *
 * <p>Incoming values are validated against a strict pattern to prevent log injection (e.g. newlines).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";
    private static final Pattern SAFE_ID = Pattern.compile("^[A-Za-z0-9-]{8,64}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String correlationId = resolve(request.getHeader(HEADER));
        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER, correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    static String resolve(String incoming) {
        if (incoming != null && SAFE_ID.matcher(incoming).matches()) {
            return incoming;
        }
        return UUID.randomUUID().toString();
    }
}
