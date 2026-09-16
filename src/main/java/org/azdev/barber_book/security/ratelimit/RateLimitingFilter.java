package org.azdev.barber_book.security.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.azdev.barber_book.dtos.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitingService rateLimitingService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (isPublicAppointmentRequest(request)) {
            String clientIp = request.getRemoteAddr();
            if (clientIp == null || clientIp.isBlank()) {
                clientIp = "unknown";
            }

            var bucket = rateLimitingService.resolveBucket(clientIp);

            if (!bucket.tryConsume(1)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setCharacterEncoding("UTF-8");
                response.setContentType("application/json");
                response.setHeader("Retry-After", "3600");

                ApiErrorResponse error = new ApiErrorResponse(
                        OffsetDateTime.now(),
                        HttpStatus.TOO_MANY_REQUESTS.value(),
                        HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                        "Você excedeu o limite de agendamentos. Tente novamente mais tarde.",
                        request.getRequestURI()
                );

                response.getWriter().write("{\"timestamp\":\"" + error.timestamp() + "\",\"status\":" + error.status() + ",\"error\":\"" + error.error() + "\",\"message\":\"" + error.message() + "\",\"path\":\"" + error.path() + "\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicAppointmentRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI().startsWith("/api/v1/public/appointments");
    }
}