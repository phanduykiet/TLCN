package com.sc.scifunapi.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "user_onboardings")
public class UserOnboarding {

    @Id
    private String id;

    private String userId;        // ref tới User

    private String subject;       // Bước 1: "Lý", "Hóa", "Sinh"...

    private String ageGroup;      // Bước 2: "<5", "5-8", "9-12", "13-15", "16-18", "+18"

    private String referralSource; // Bước 3: "Facebook", "TikTok", "YouTube", "Instagram", "Bạn bè", "Khác"

    private String level;

    @CreatedDate
    private Date createdAt;
}