package org.azdev.barber_book.services;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.azdev.barber_book.security.AuthenticatedUserPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {
    @Value("${api.security.token.secret}")
    private String secret;

    @Value("${api.security.token.expiration}")
    private Long expiration;

    private SecretKey getSignInKey() {
        byte[] keyBytes;
        try {
            // Try standard Base64 decode (common when SECRET is provided as Base64)
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (io.jsonwebtoken.io.DecodingException ex1) {
            try {
                // Try URL-safe Base64 (may contain '-' and '_')
                keyBytes = Decoders.BASE64URL.decode(secret);
            } catch (io.jsonwebtoken.io.DecodingException ex2) {
                // Fallback: treat secret as a raw string and use its UTF-8 bytes
                keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            }
        }

        // Ensure the key has sufficient length for HMAC (at least 256 bits for HS256)
        if (keyBytes.length < 32) {
            try {
                MessageDigest sha = MessageDigest.getInstance("SHA-256");
                keyBytes = sha.digest(keyBytes);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to generate signing key", e);
            }
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(AuthenticatedUserPrincipal user) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("tenantId", user.tenantId().toString());

        return Jwts.builder()
                .claims(extraClaims)
                .subject(user.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractTenantId(String token) {
        return extractClaim(token, claims -> claims.get("tenantId", String.class));
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claimsResolver.apply(claims);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }
}
