package com.sc.scifunapi.config;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Chưa đăng nhập (không có Authentication) → 401
    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public Map<String, Object> handleAuthRequired(AuthenticationCredentialsNotFoundException ex) {
        return Map.of(
                "status", HttpStatus.UNAUTHORIZED.value(),
                "message", "Vui lòng đăng nhập để tiếp tục"
        );
    }

    // Đã đăng nhập nhưng thiếu quyền → 403
    @ExceptionHandler(AccessDeniedException.class)
    public Map<String, Object> handleAccessDenied(AccessDeniedException ex) {
        return Map.of(
                "status", HttpStatus.FORBIDDEN.value(),
                "message", "Bạn không có quyền truy cập tài nguyên này"
        );
    }

    // Token hết hạn → 401
    @ExceptionHandler(ExpiredJwtException.class)
    public Map<String, Object> handleJwtExpired(ExpiredJwtException ex) {
        return Map.of(
                "status", HttpStatus.UNAUTHORIZED.value(),
                "message", "Token đã hết hạn, vui lòng đăng nhập lại"
        );
    }

    // Token không hợp lệ (sai chữ ký, format...) → 400
    @ExceptionHandler(JwtException.class)
    public Map<String, Object> handleJwtInvalid(JwtException ex) {
        return Map.of(
                "status", HttpStatus.BAD_REQUEST.value(),
                "message", "Token không hợp lệ"
        );
    }
}
