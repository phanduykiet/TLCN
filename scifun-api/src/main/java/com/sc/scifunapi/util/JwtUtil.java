package com.sc.scifunapi.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    private final SecretKey jwtKey;

    public JwtUtil(SecretKey jwtKey) {
        this.jwtKey = jwtKey;
    }

    @Value("${app.jwt.expiresSeconds:3600}")
    private long expiresSeconds;

    public String generateToken(String userId, String email, String role) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expiresSeconds);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("role", role);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(jwtKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public io.jsonwebtoken.Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(jwtKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
