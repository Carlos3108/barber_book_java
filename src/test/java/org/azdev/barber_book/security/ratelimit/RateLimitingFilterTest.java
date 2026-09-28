package org.azdev.barber_book.security.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    @Mock
    private RateLimitingService rateLimitingService;

    @Mock
    private FilterChain filterChain;

    private RateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitingFilter(rateLimitingService);
    }

    private Bucket bucketWithCapacity(int capacity) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(capacity).refillIntervally(capacity, Duration.ofMinutes(60)).build())
                .build();
    }

    @Test
    void allowsAppointmentRequestWhenUnderLimit() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/public/appointments");
        request.setRemoteAddr("1.1.1.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(rateLimitingService.resolveBucket("1.1.1.1")).thenReturn(bucketWithCapacity(5));

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void blocksAppointmentRequestWhenLimitExceeded() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/public/appointments");
        request.setRemoteAddr("1.1.1.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Bucket bucket = bucketWithCapacity(1);
        bucket.tryConsume(1);
        when(rateLimitingService.resolveBucket("1.1.1.1")).thenReturn(bucket);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
        verify(filterChain, never()).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
    }

    @Test
    void blocksLoginRequestWhenLimitExceeded() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("2.2.2.2");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Bucket bucket = bucketWithCapacity(1);
        bucket.tryConsume(1);
        when(rateLimitingService.resolveLoginBucket("2.2.2.2")).thenReturn(bucket);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    void blocksRegisterRequestWhenLimitExceeded() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/register");
        request.setRemoteAddr("3.3.3.3");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Bucket bucket = bucketWithCapacity(1);
        bucket.tryConsume(1);
        when(rateLimitingService.resolveLoginBucket("3.3.3.3")).thenReturn(bucket);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    void bypassesRateLimitForOtherPaths() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/services");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void usesUnknownIpWhenRemoteAddrIsBlank() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/public/appointments");
        request.setRemoteAddr("");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(rateLimitingService.resolveBucket("unknown")).thenReturn(bucketWithCapacity(5));

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    private static <T> T any(Class<T> type) {
        return org.mockito.ArgumentMatchers.any(type);
    }
}
