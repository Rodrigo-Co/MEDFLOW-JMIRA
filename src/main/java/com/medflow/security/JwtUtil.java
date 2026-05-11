package com.medflow.security;

import com.medflow.config.AppConfig;
import com.medflow.http.AuthenticatedUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Optional;

public class JwtUtil {

    private final String secret;
    private final long expirationMs;

    public JwtUtil(AppConfig config) {
        this.secret = config.getRequired("jwt.secret");
        this.expirationMs = config.getLong("jwt.expiration-ms", 86_400_000L);
    }

    public String generate(String userId, String role) {
        return Jwts.builder()
                .setSubject(userId)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Optional<AuthenticatedUser> parse(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return Optional.of(new AuthenticatedUser(claims.getSubject(), String.valueOf(claims.get("role"))));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private Key key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
