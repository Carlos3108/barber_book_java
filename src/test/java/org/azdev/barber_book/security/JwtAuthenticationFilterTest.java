package org.azdev.barber_book.security;

import io.jsonwebtoken.ExpiredJwtException;
import org.azdev.barber_book.services.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private UserDetailsService userDetailsService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldNotFilterAuthRoutes() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/auth/login");

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void doesNothingWhenAuthorizationHeaderIsMissing() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        verify(jwtService, never()).extractUsername(org.mockito.ArgumentMatchers.anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void ignoresExpiredTokenAndKeepsRequestFlow() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer expired-token");

        when(jwtService.extractUsername("expired-token")).thenThrow(new ExpiredJwtException(null, null, "expired"));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void setsSecurityContextForValidToken() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");

        UserDetails userDetails = User.withUsername("owner@test.com").password("x").roles("ADMIN").build();
        when(jwtService.extractUsername("valid-token")).thenReturn("owner@test.com");
        when(userDetailsService.loadUserByUsername("owner@test.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("valid-token", userDetails)).thenReturn(true);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("owner@test.com");
    }

    @Test
    void ignoresInvalidTokenAndKeepsRequestFlow() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer malformed-token");

        when(jwtService.extractUsername("malformed-token")).thenThrow(new RuntimeException("bad token"));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void rejectsSuspendedAccountEvenWithValidToken() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer suspended-token");

        UserDetails suspendedUser = org.mockito.Mockito.mock(UserDetails.class);
        when(suspendedUser.isAccountNonLocked()).thenReturn(false);

        when(jwtService.extractUsername("suspended-token")).thenReturn("suspended@test.com");
        when(userDetailsService.loadUserByUsername("suspended@test.com")).thenReturn(suspendedUser);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void skipsAuthenticationWhenTokenIsInvalidForUser() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer wrong-signature-token");

        UserDetails userDetails = User.withUsername("owner@test.com").password("x").roles("ADMIN").build();
        when(jwtService.extractUsername("wrong-signature-token")).thenReturn("owner@test.com");
        when(userDetailsService.loadUserByUsername("owner@test.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("wrong-signature-token", userDetails)).thenReturn(false);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void ignoresUsernameNotFoundException() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer unknown-user-token");

        when(jwtService.extractUsername("unknown-user-token")).thenReturn("ghost@test.com");
        when(userDetailsService.loadUserByUsername("ghost@test.com"))
                .thenThrow(new org.springframework.security.core.userdetails.UsernameNotFoundException("not found"));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void ignoresGenericExceptionDuringAuthentication() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer error-token");

        when(jwtService.extractUsername("error-token")).thenReturn("owner@test.com");
        when(userDetailsService.loadUserByUsername("owner@test.com")).thenThrow(new RuntimeException("db down"));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void skipsReAuthenticationWhenAlreadyAuthenticated() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer already-authed-token");

        org.springframework.security.authentication.TestingAuthenticationToken existing =
                new org.springframework.security.authentication.TestingAuthenticationToken("existing", "pwd");
        SecurityContextHolder.getContext().setAuthentication(existing);

        when(jwtService.extractUsername("already-authed-token")).thenReturn("owner@test.com");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing);
        verify(userDetailsService, never()).loadUserByUsername(org.mockito.ArgumentMatchers.anyString());
    }
}

