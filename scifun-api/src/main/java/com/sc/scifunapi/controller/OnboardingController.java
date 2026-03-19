package com.sc.scifunapi.controller;

import com.sc.scifunapi.dto.UserOnboarding.OnboardingRequest;
import com.sc.scifunapi.service.OnboardingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<Map<String, Object>> saveOnboarding(
            @RequestBody OnboardingRequest request
    ) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getName() == null) {
                return ResponseEntity.status(401).body(
                        Map.of(
                                "status", 401,
                                "message", "Không tìm thấy thông tin người dùng từ token"
                        )
                );
            }

            String userId = (String) auth.getDetails();

            onboardingService.saveOnboarding(userId, request);

            return ResponseEntity.ok(
                    Map.of(
                            "status", 200,
                            "message", "Lưu thông tin onboarding thành công"
                    )
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "status", 400,
                            "message", e.getMessage()
                    )
            );
        }
    }
}