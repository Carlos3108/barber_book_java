package org.azdev.barber_book.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.azdev.barber_book.services.JwtService;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor // O Lombok vai criar o construtor injetando as dependências `final`
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Pega o cabeçalho de autorização da requisição
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // 2. Se não tem cabeçalho ou não começa com "Bearer ", passa direto (deixa o Spring bloquear depois se a rota for privada)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extrai o token (tirando os 7 caracteres do "Bearer ")
        jwt = authHeader.substring(7);

        // 4. Extrai o e-mail (subject) de dentro do token
        try{
            userEmail = jwtService.extractUsername(jwt);
        } catch (io.jsonwebtoken.ExpiredJwtException ex) {
            filterChain.doFilter(request, response);
            return;
        }

        // 5. Se tem e-mail no token e o usuário ainda não está logado no contexto atual do Spring...
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Vai no banco buscar os dados do usuário
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            // 6. Se o token for válido e bater com o usuário do banco...
            if (jwtService.isTokenValid(jwt, userDetails)) {

                // Cria o objeto de autenticação oficial do Spring
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                // Adiciona detalhes da requisição (IP, sessão, etc)
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 7. Salva o usuário no cofre de segurança do Spring (SecurityContext)
                SecurityContextHolder.getContext().setAuthentication(authToken);

                // 🚀 PULO DO GATO SÊNIOR (Para o futuro):
                // Aqui nós podemos extrair o tenantId do token e colocar num TenantContext (ThreadLocal)
                //String tenantId = jwtService.extractTenantId(jwt);
                //TenantContext.setCurrentTenant(tenantId);
            }
        }

        // 8. Libera a requisição para continuar o fluxo
        filterChain.doFilter(request, response);
    }
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/api/auth/");
    }
}