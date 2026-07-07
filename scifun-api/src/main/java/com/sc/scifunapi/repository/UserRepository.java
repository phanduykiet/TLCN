package com.sc.scifunapi.repository;

import com.sc.scifunapi.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    // Tìm user theo email (dùng cho đăng ký / đăng nhập)
    Optional<User> findByEmail(String email);

    // Kiểm tra email đã tồn tại chưa
    boolean existsByEmail(String email);

    // Tìm user theo OTP (dùng cho verify OTP)
    Optional<User> findByOtp(String otp);

    // Tìm theo trạng thái verify
    List<User> findByIsVerified(boolean isVerified);

    // Tìm user có role cụ thể (USER / ADMIN)
    List<User> findByRole(String role);

    List<User> findTop5ByOrderByCreatedAtDesc();
}
