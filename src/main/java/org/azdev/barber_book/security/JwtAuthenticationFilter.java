package org.azdev.barber_book.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.azdev.barber_book.services.JwtService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        try {
            userEmail = jwtService.extractUsername(jwt);
            log.debug("JWT extraído com sucesso. Email: {}", userEmail);
        } catch (io.jsonwebtoken.ExpiredJwtException ex) {
            log.warn("Token JWT expirado para requisição: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        } catch (Exception ex) {
            log.error("Erro ao extrair username do JWT: {}", ex.getMessage(), ex);
            filterChain.doFilter(request, response);
            return;
        }

        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            try {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                log.debug("Usuário carregado: {}", userEmail);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    log.debug("Token válido para usuário: {}", userEmail);

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("Autenticação setada no contexto para: {}", userEmail);
                } else {
                    log.warn("Token inválido para usuário: {}", userEmail);
                }
            } catch (org.springframework.security.core.userdetails.UsernameNotFoundException ex) {
                log.warn("Usuário não encontrado: {}", userEmail);
            } catch (Exception ex) {
                log.error("Erro ao autenticar usuário: {}", ex.getMessage(), ex);
            }
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/api/auth/");
    }
}