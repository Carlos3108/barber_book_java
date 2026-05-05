package org.azdev.barber_book.config;

import org.azdev.barber_book.security.JwtAuthenticationFilter;
import org.azdev.barber_book.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    @Test
    void corsConfigurationAllowsFrontendAndExpectedMethods() {
        SecurityConfig config = new SecurityConfig(
                Mockito.mock(JwtAuthenticationFilter.class)
        );

        CorsConfigurationSource source = config.corsConfigurationSource();
        CorsConfiguration cors = source.getCorsConfiguration(new MockHttpServletRequest());

        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOrigins()).contains("http://localhost:3000");
        assertThat(cors.getAllowedMethods()).contains("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        assertThat(cors.getAllowCredentials()).isTrue();
    }
}

