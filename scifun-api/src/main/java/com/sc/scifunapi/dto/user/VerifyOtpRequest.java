package com.sc.scifunapi.dto.user;

import lombok.Data;

@Data
public class VerifyOtpRequest {
    private String email;
    private String otp;
}
