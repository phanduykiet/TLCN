package com.sc.scifunapi.dto.user;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Service
public class JwtService {

    private final SecretKey key;

    public JwtService(@Value("${app.jwt.secret}") String secret) {
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    public JwtUser verify(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        String userId = String.valueOf(claims.get("userId"));
        String email = String.valueOf(claims.get("email"));
        String role  = String.valueOf(claims.get("role"));

        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Invalid token claims: missing userId");
        }

        return new JwtUser(userId, email, role);
    }
}

