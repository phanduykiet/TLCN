package com.sc.scifunapi.config;

import com.sc.scifunapi.middleware.JwtAuthFilter;
import com.sc.scifunapi.middleware.OptionalAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final OptionalAuthFilter optionalAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ❌ Tắt CSRF vì mình xài API REST
                .csrf(csrf -> csrf.disable())

                // ✅ Cho phép tất cả request — quyền sẽ kiểm tra bằng @PreAuthorize
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/**").permitAll()
                        .anyRequest().permitAll()
                )

                // ✅ OptionalAuthFilter chạy TRƯỚC JwtAuthFilter
                .addFilterBefore(optionalAuthFilter, UsernamePasswordAuthenticationFilter.class)

                // ✅ Thêm JWT filter trước UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
