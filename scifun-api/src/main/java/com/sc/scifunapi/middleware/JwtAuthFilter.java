package com.sc.scifunapi.middleware;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sc.scifunapi.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String authHeader = req.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(req, res);
            return;
        }

        String token = authHeader.substring(7).trim();

        try {
            var claims = jwtUtil.parseToken(token);
            var role = claims.get("role", String.class);
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

            var authentication = new UsernamePasswordAuthenticationToken(
                    claims.get("email", String.class),
                    null,
                    authorities
            );

            authentication.setDetails(claims.get("userId", String.class));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            chain.doFilter(req, res);

        } catch (ExpiredJwtException e) {
            sendError(res, 401, "Token đã hết hạn, vui lòng đăng nhập lại");
        } catch (JwtException | BadCredentialsException e) {
            sendError(res, 400, "Token không hợp lệ");
        } catch (AuthenticationCredentialsNotFoundException e) {
            sendError(res, 401, "Vui lòng đăng nhập để tiếp tục");
        }
    }

    private void sendError(HttpServletResponse res, int status, String message) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");

        var body = Map.of(
                "status", status,
                "message", message
        );

        var out = res.getOutputStream();
        new ObjectMapper().writeValue(out, body);
        out.flush();
    }
}
