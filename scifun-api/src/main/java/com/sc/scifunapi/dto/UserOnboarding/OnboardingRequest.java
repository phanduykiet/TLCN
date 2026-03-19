package com.sc.scifunapi.dto.UserOnboarding;

import lombok.Data;

@Data
public class OnboardingRequest {
    private String subject;        // Bước 1
    private String ageGroup;       // Bước 2
    private String referralSource; // Bước 3
    private String level;
}