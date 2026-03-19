package com.sc.scifunapi.entity;

import com.sc.scifunapi.enums.*;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String password;

    @Builder.Default
    private String fullname = "New User";

    private String otp;
    private Date otpExpires;

    @Builder.Default
    private boolean isVerified = false;

    @Builder.Default
    private String avatar =
            "https://res.cloudinary.com/dglm2f7sr/image/upload/v1761373988/default_awmzq0.jpg";

    @Builder.Default
    private Role role = Role.USER;

    // giữ kiểu Date giống Mongo cũ (JS Date)
    @Builder.Default
    private Date dob = new Date(946684800000L); // 2000-01-01 UTC

    // giữ đúng 0|1 như schema cũ
    @Builder.Default
    private Integer sex = 1; // 0 = female, 1 = male

    @Builder.Default
    private Subscription subscription = Subscription.builder()
            .status(SubscriptionStatus.NONE)
            .build();

    @Builder.Default
    private boolean isFirstLogin = true;

    @Builder.Default
    private Date createdAt = new Date();

    private Date expiredAt; // null = user thật | now + 30 ngày = guest
}
