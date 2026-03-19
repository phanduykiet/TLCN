package com.sc.scifunapi.service;
import com.sc.scifunapi.dto.UserOnboarding.OnboardingRequest;
import com.sc.scifunapi.entity.UserOnboarding;
import com.sc.scifunapi.repository.UserOnboardingRepository;
import com.sc.scifunapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.token.TokenService;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final UserOnboardingRepository userOnboardingRepository;
    private final UserRepository userRepository;

    public void saveOnboarding(String userId, OnboardingRequest request) {
        UserOnboarding onboarding = userOnboardingRepository.findByUserId(userId)
                .orElse(UserOnboarding.builder().userId(userId).build());

        onboarding.setSubject(request.getSubject());
        onboarding.setAgeGroup(request.getAgeGroup());
        onboarding.setReferralSource(request.getReferralSource());
        onboarding.setLevel(request.getLevel());
        System.out.println("request.getLevel()");

        userOnboardingRepository.save(onboarding);

        // ── Tính dob từ ageGroup rồi update User ──────────────────
        userRepository.findById(userId).ifPresent(user -> {
            int currentYear = LocalDate.now().getYear();
            int birthYear;

            switch (request.getAgeGroup()) {
                case "<5"   -> birthYear = currentYear - 3;  // giữa khoảng
                case "5-8"  -> birthYear = currentYear - 6;
                case "9-12" -> birthYear = currentYear - 10;
                case "13-15"-> birthYear = currentYear - 14;
                case "16-18"-> birthYear = currentYear - 17;
                case "+18"  -> birthYear = currentYear - 20;
                default     -> birthYear = currentYear - 18; // fallback
            }

            Date dob = Date.from(LocalDate.of(birthYear, 1, 1)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant());

            user.setDob(dob);
            userRepository.save(user);
        });
    }
}