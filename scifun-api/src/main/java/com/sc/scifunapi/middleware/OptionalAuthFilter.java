package com.sc.scifunapi.middleware;

import com.sc.scifunapi.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * OptionalAuthFilter:
 *  - Nếu KHÔNG có token hoặc token sai → KHÔNG throw lỗi, cho qua như anonymous.
 *  - Nếu token hợp lệ → extract userId, email, role và gắn vào request attribute.
 *
 *  Controller có thể lấy:
 *      String userId = (String) request.getAttribute("optionalUserId");
 *      String email  = (String) request.getAttribute("optionalUserEmail");
 *      String role   = (String) request.getAttribute("optionalUserRole");
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OptionalAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Không có header hoặc sai format → bỏ qua, user = null
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            setAnonymous(request);
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            setAnonymous(request);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Parse token, KHÔNG throw ra ngoài
            Claims claims = jwtUtil.parseToken(token);

            String userId = claims.get("userId", String.class);
            String email  = claims.get("email", String.class);
            String role   = claims.get("role", String.class);

            // Nếu parse ok thì gắn thông tin user vào request
            request.setAttribute("optionalUserId",   userId);
            request.setAttribute("optionalUserEmail", email);
            request.setAttribute("optionalUserRole",  role);

        } catch (ExpiredJwtException e) {
            log.warn("OptionalAuthFilter - token expired: {}", e.getMessage());
            setAnonymous(request);
        } catch (JwtException | IllegalArgumentException e) {
            // Sai chữ ký, token hỏng, v.v...
            log.warn("OptionalAuthFilter - invalid token: {}", e.getMessage());
            setAnonymous(request);
        }

        // Dù token đúng hay sai cũng luôn cho request đi tiếp
        filterChain.doFilter(request, response);
    }

    private void setAnonymous(HttpServletRequest request) {
        request.setAttribute("optionalUserId", null);
        request.setAttribute("optionalUserEmail", null);
        request.setAttribute("optionalUserRole", null);
    }
}
