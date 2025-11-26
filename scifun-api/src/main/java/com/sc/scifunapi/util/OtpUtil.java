package com.sc.scifunapi.util;

import java.util.Random;

public class OtpUtil {
    private static final Random RND = new Random();

    // Tạo mã OTP 6 số
    public static String generateOTP() {
        int n = 100000 + RND.nextInt(900000);
        return Integer.toString(n);
    }
}
