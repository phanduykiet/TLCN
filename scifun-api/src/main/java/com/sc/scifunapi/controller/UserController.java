package com.sc.scifunapi.controller;

import com.sc.scifunapi.dto.user.LoginRequest;
import com.sc.scifunapi.dto.user.RegisterRequest;
import com.sc.scifunapi.dto.user.VerifyOtpRequest;
import com.sc.scifunapi.entity.User;
import com.sc.scifunapi.repository.UserRepository;
import com.sc.scifunapi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    // Đăng ký tài khoản
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest body) {
        try {
            var data = userService.register(body);
            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Đăng ký thành công. Vui lòng kiểm tra email để lấy OTP.",
                    "data", data
            ));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(400).body(Map.of(  // 409 CONFLICT hợp lý hơn 500
                    "status", 400,
                    "message", ex.getMessage()
            ));
        }
    }

    // Xác thực OTP
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody VerifyOtpRequest body) {
        try {
            userService.verifyOtp(body.getEmail(), body.getOtp());
            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Xác thực thành công"
            ));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(400).body(Map.of(
                    "status", 400,
                    "message", ex.getMessage()
            ));
        }
    }

    // Đăng nhập có jwt
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest body) {
        try {
            var result = userService.login(body); // { token, user }

            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Đăng nhập thành công",
                    "token", result.get("token"),
                    "data", result.get("user")
            ));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(400).body(Map.of(
                    "status", 400,
                    "message", ex.getMessage()
            ));
        }
    }

    // Lấy lại mật khẩu
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@RequestBody Map<String, String> req) {
        try {
            String email = req.get("email");
            userService.forgotPassword(email);
            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "OTP đã được gửi đến email"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400,
                    "message", e.getMessage()
            ));
        }
    }

    // Cập nhật lại mật khẩu (Lấy lại mật khẩu)
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> req) {
        try {
            String email = req.get("email");
            String newPassword = req.get("newPassword");
            userService.resetPassword(email, newPassword);
            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Mật khẩu đã được cập nhật"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(Map.of(
                    "status", 400,
                    "message", e.getMessage()
            ));
        }
    }

    // Cập nhật mật khẩu: chỉ USER/ADMIN được gọi
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PutMapping("/update-password/{_id}")
    public ResponseEntity<?> updatePassword(
            @PathVariable("_id") String id,
            @RequestBody Map<String, String> body
    ) {
        try {
            String oldPassword = body.get("oldPassword");
            String newPassword = body.get("newPassword");
            String confirmPassword = body.get("confirmPassword"); // giữ đúng chữ, không thêm validate

            userService.updatePassword(id, oldPassword, newPassword, confirmPassword);

            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Cập nhật mật khẩu thành công"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(Map.of(
                    "status", 400,
                    "message", e.getMessage()
            ));
        }
    }

    // Cập nhật thông tin người dùng (form-data)
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PutMapping(value = "/update-user/{_id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateUser(
            @PathVariable("_id") String id,
            @RequestParam Map<String, String> form,                   // các field text gửi cùng form-data
            @RequestPart(value = "avatar", required = false) MultipartFile avatar // file "avatar" (optional)
    ) {
        try {
            // userId được set ở JwtAuthFilter: authentication.setDetails(userId)
            String authUserId = (String) SecurityContextHolder.getContext().getAuthentication().getDetails();

            User updated = userService.updateUser(id, form, avatar, authUserId);

            // sanitize trả về giống select("-password -otp -otpExpires")
            Map<String, Object> data = new HashMap<>();
            data.put("id", updated.getId());
            data.put("email", updated.getEmail());
            data.put("fullname", updated.getFullname());
            data.put("isVerified", updated.isVerified());
            data.put("avatar", updated.getAvatar());
            data.put("role", updated.getRole());
            data.put("dob", updated.getDob());
            data.put("sex", updated.getSex());
            data.put("subscription", updated.getSubscription());
            data.put("level", userService.getUserLevel(updated.getId())); // thêm mới

            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Cập nhật thành công",
                    "data", data
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(Map.of(
                    "status", 400,
                    "message", e.getMessage()
            ));
        }
    }

    // Tạo user mới (chỉ ADMIN)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create-user")
    public ResponseEntity<?> createUser(@RequestBody Map<String, Object> body) {
        try {
            User created = userService.createUserByAdmin(body);

            // ẩn trường nhạy cảm giống .select("-password -otp -otpExpires")
            Map<String, Object> data = Map.of(
                    "id", created.getId(),
                    "email", created.getEmail(),
                    "fullname", created.getFullname(),
                    "isVerified", created.isVerified(),
                    "avatar", created.getAvatar(),
                    "role", created.getRole(),
                    "dob", created.getDob(),
                    "sex", created.getSex(),
                    "subscription", created.getSubscription()
            );

            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Tạo tài khoản thành công",
                    "data", data
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400,
                    "message", e.getMessage()
            ));
        }
    }

    // Xóa người dùng
    @DeleteMapping("/delete-user/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable("id") String id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Xóa người dùng thành công"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400,
                    "message", e.getMessage()
            ));
        }
    }

    // Lấy chi tiết
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/get-user/{id}")
    public ResponseEntity<?> getInfoUser(@PathVariable("id") String id) {
        try {
            // userId đã set ở JwtAuthFilter: authentication.setDetails(userId)
            String authUserId = (String) SecurityContextHolder.getContext()
                    .getAuthentication().getDetails();

            Map<String, Object> user = userService.getInfoUser(id, authUserId);

            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Lấy thông tin người dùng thành công",
                    "data", user
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400,
                    "message", e.getMessage()
            ));
        }
    }

    //  Lấy danh sách người dùng
    @GetMapping("/get-user-list")
    public ResponseEntity<?> getUserList(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String search
    ) {
        try {
            var data = userService.getUserList(page, limit, search);
            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Lấy danh sách người dùng thành công",
                    "data", data
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(Map.of(
                    "status", 400,
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/guest/refresh")
    public ResponseEntity<?> refreshGuest(@RequestBody Map<String, String> body) {
        try {
            var data = userService.refreshGuestToken(body.get("userId"));
            return ResponseEntity.ok(Map.of("status", 200, "data", data));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(401).body(Map.of("status", 401, "message", ex.getMessage()));
        }
    }

    @PostMapping("/guest")
    public ResponseEntity<?> createGuest() {
        try {
            var data = userService.createGuest();
            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Tạo phiên khách thành công",
                    "data", data
            ));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(400).body(Map.of(
                    "status", 400,
                    "message", ex.getMessage()
            ));
        }
    }

    @PostMapping("/guest/convert")
    public ResponseEntity<?> convertGuest(@RequestBody RegisterRequest body) {
        try {
            // Lấy userId từ token hiện tại
            var auth = SecurityContextHolder.getContext().getAuthentication();
            String userId = (String) auth.getDetails();

            var data = userService.convertGuestToUser(userId, body);
            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Vui lòng kiểm tra email để lấy OTP xác thực",
                    "data", data
            ));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(400).body(Map.of(
                    "status", 400,
                    "message", ex.getMessage()
            ));
        }
    }
}