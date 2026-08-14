package io.iprf.transaction.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Assigns a correlation ID to every request and puts it on the logging context.
 *
 * <p>The ID is persisted on the audit record and carried on every event
 * published downstream. Without it, tracing a decision across the async boundary
 * — where the enrichment and post-settlement work happens minutes or hours later
 * — means correlating by timestamp and hoping.
 *
 * <p>An inbound {@code X-Correlation-Id} is honoured so a caller's trace survives
 * into this system.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";
    public static final String REQUEST_ATTRIBUTE = "iprf.correlationId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String correlationId = resolve(request);
        MDC.put(MDC_KEY, correlationId);
        request.setAttribute(REQUEST_ATTRIBUTE, correlationId);
        response.setHeader(HEADER, correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            // Servlet threads are pooled. Leaving the MDC populated would leak
            // this request's ID into the next request served by this thread.
            MDC.remove(MDC_KEY);
        }
    }

    private static String resolve(HttpServletRequest request) {
        String inbound = request.getHeader(HEADER);
        return inbound == null || inbound.isBlank()
                ? UUID.randomUUID().toString()
                : inbound.trim();
    }

    /** Reads the correlation ID assigned to the current request. */
    public static String current(HttpServletRequest request) {
        Object value = request.getAttribute(REQUEST_ATTRIBUTE);
        return value instanceof String s ? s : UUID.randomUUID().toString();
    }
}
