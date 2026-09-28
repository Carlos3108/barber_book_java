package org.azdev.barber_book.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RequestTimingFilterTest {

    private final RequestTimingFilter filter = new RequestTimingFilter();

    @Test
    void doesNotLogWhenRequestIsFast() throws Exception {
        ReflectionTestUtils.setField(filter, "slowRequestThresholdMs", 60_000L);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/services");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void logsWarningWhenRequestIsSlow() throws Exception {
        ReflectionTestUtils.setField(filter, "slowRequestThresholdMs", 0L);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/services");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
