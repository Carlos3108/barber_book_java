package org.azdev.barber_book.security;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RequestMetricsFilterTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final RequestMetricsFilter filter = new RequestMetricsFilter(meterRegistry);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doesNotRecordMetricWhenStatusIsBelow400() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/services");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        assertThat(meterRegistry.find("app.http.errors").counter()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void recordsMetricWithAnonymousTagWhenUnauthenticated() throws Exception {
        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/services");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(404);
        FilterChain filterChain = (req, res) -> ((MockHttpServletResponse) res).setStatus(404);

        filter.doFilter(request, response, filterChain);

        assertThat(meterRegistry.find("app.http.errors").tag("tenant", "anonymous").counter()).isNotNull();
    }

    @Test
    void recordsMetricWithTenantTagWhenAuthenticated() throws Exception {
        UUID tenantId = UUID.randomUUID();
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                UUID.randomUUID(), "user@test.com", "pw", tenantId, "ACTIVE");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/appointments");
        FilterChain filterChain = (req, res) -> ((MockHttpServletResponse) res).setStatus(500);

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        assertThat(meterRegistry.find("app.http.errors").tag("tenant", tenantId.toString()).counter()).isNotNull();
    }

    @Test
    void treatsBlankUriAsUnknown() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "");
        FilterChain filterChain = (req, res) -> ((MockHttpServletResponse) res).setStatus(400);

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        assertThat(meterRegistry.find("app.http.errors").tag("uri", "unknown").counter()).isNotNull();
    }
}
