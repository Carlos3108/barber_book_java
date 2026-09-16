package org.azdev.barber_book.security;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class RequestMetricsFilter extends OncePerRequestFilter {

    private final MeterRegistry meterRegistry;

    public RequestMetricsFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } finally {
            int status = response.getStatus();
            if (status >= 400) {
                String tenantId = resolveTenantId();
                Counter.builder("app.http.errors")
                        .tag("method", request.getMethod())
                        .tag("uri", normalizeUri(request.getRequestURI()))
                        .tag("status", String.valueOf(status))
                        .tag("tenant", tenantId == null ? "anonymous" : tenantId)
                        .register(meterRegistry)
                        .increment();
            }
        }
    }

    private String resolveTenantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUserPrincipal userPrincipal) {
            UUID tenantId = userPrincipal.tenantId();
            return tenantId == null ? null : tenantId.toString();
        }

        return null;
    }

    private String normalizeUri(String uri) {
        if (uri == null || uri.isBlank()) {
            return "unknown";
        }
        return uri;
    }
}
