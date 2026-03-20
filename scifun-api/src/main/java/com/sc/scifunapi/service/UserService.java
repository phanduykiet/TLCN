package com.sc.scifunapi.service;

import com.sc.scifunapi.dto.user.LoginRequest;
import com.sc.scifunapi.dto.user.RegisterRequest;
import com.sc.scifunapi.entity.Subscription;
import com.sc.scifunapi.entity.User;
import com.sc.scifunapi.entity.UserOnboarding;
import com.sc.scifunapi.enums.Role;
import com.sc.scifunapi.enums.SubscriptionStatus;
import com.sc.scifunapi.repository.*;
import com.sc.scifunapi.util.OtpUtil;
import com.sc.scifunapi.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserService {
    @Autowired
    private JwtUtil jwtUtil;
    private final MongoTemplate mongoTemplate;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final CloudinaryService cloudinaryService;
    private final UserProgressRepository userProgressRepository;
    private final UserOnboardingRepository userOnboardingRepository;
    private final SubmissionRepository submissionRepository;
    private final ResultRepository resultRepository;
    private final LeaderboardRepository leaderboardRepository;
    private final FavoriteQuizRepository favoriteQuizRepository;
    public record UserStatusResult(boolean isGuest) {}

    // Đăng ký tài khoản kèm gửi OTP
    public Map<String, Object> register(RegisterRequest req) {
        var existing = userRepository.findByEmail(req.getEmail()).orElse(null);
        User user;

        if (existing != null) {
            if (existing.isVerified()) {
                throw new RuntimeException("Email đã được sử dụng, vui lòng đăng nhập");
            }
            existing.setPassword(passwordEncoder.encode(req.getPassword()));
            existing.setOtp(OtpUtil.generateOTP());
            existing.setOtpExpires(new Date(System.currentTimeMillis() + 5 * 60 * 1000));
            user = existing;
        } else {
            user = User.builder()
                    .email(req.getEmail())
                    .password(passwordEncoder.encode(req.getPassword()))
                    .fullname(req.getFullname() != null ? req.getFullname() : "New User")
                    .otp(OtpUtil.generateOTP())
                    .otpExpires(new Date(System.currentTimeMillis() + 5 * 60 * 1000))
                    .isVerified(false)
                    .role(Role.USER)
                    .subscription(Subscription.builder().status(SubscriptionStatus.NONE).build())
                    .build();
        }

        // Gửi OTP qua email
        mailService.sendMail(
                user.getEmail(),
                "OTP xác thực đăng ký",
                "Mã OTP của bạn là: " + user.getOtp()
        );

        userRepository.save(user);

        Map<String, Object> res = new HashMap<>();
        res.put("id", user.getId());
        res.put("email", user.getEmail());
        res.put("fullname", user.getFullname());
        res.put("isVerified", user.isVerified());
        res.put("role", user.getRole());
        return res;
    }

    // Xác thực OTP
    public void verifyOtp(String email, String otp) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        Date now = new Date();

        // OTP sai hoặc hết hạn
        if (user.getOtp() == null || !user.getOtp().equals(otp)
                || user.getOtpExpires() == null || user.getOtpExpires().before(now)) {
            throw new RuntimeException("OTP không hợp lệ hoặc đã hết hạn");
        }

        // Xác thực thành công
        user.setVerified(true);       // field boolean isVerified -> setter là setVerified(...)
        user.setOtp("");              // xoá OTP
        user.setOtpExpires(new Date(0)); // xoá hạn
        userRepository.save(user);
    }

    // Đăng nhập có jwt
    public Map<String, Object> login(LoginRequest req) {
        var user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("Email không tồn tại"));

        if (!Boolean.TRUE.equals(user.isVerified())) {
            throw new RuntimeException("Tài khoản chưa xác thực OTP");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("Sai mật khẩu");
        }

        // Tạo JWT (userId, email, role)
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        // Kiểm tra & cập nhật trạng thái subscription nếu đã hết hạn
        checkAndUpdateSubscriptionStatus(user);

        // Build data trả về (ẩn trường nhạy cảm)
        Map<String, Object> userData = new HashMap<>();
        userData.put("id", user.getId());
        userData.put("email", user.getEmail());
        userData.put("fullname", user.getFullname());
        userData.put("avatar", user.getAvatar());
        userData.put("role", user.getRole());
        userData.put("isVerified", user.isVerified());
        userData.put("dob", user.getDob());
        userData.put("sex", user.getSex());
        userData.put("subscription", user.getSubscription()); // có thể null/none
        userData.put("isFirstLogin", user.isFirstLogin());
        userData.put("level", getUserLevel(user.getId()));
        var onboarding = userOnboardingRepository.findByUserId(user.getId()).orElse(null);
        userData.put("level", onboarding != null ? onboarding.getLevel() : null);
        markNotFirstLogin(user.getId());
        return Map.of("token", token, "user", userData);
    }

    public String getUserLevel(String userId) {
        return userOnboardingRepository.findByUserId(userId)
                .map(UserOnboarding::getLevel)
                .orElse(null);
    }

    // Nếu đăng nhập lần 2 trở lên đổi isFirstLogin = false
    public void markNotFirstLogin(String userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setFirstLogin(false);
            userRepository.save(user);
        });
    }

    // Nếu có hạn và đã hết hạn -> chuyển về NONE
    private void checkAndUpdateSubscriptionStatus(User user) {
        var sub = user.getSubscription();
        if (sub == null || sub.getStatus() == SubscriptionStatus.NONE || sub.getCurrentPeriodEnd() == null) {
            return; // không cần cập nhật
        }

        Date now = new Date();
        if (sub.getCurrentPeriodEnd().before(now)) {
            user.setSubscription(Subscription.builder()
                    .status(SubscriptionStatus.NONE)
                    .build());
            userRepository.save(user);
        }
    }

    // Lấy lại mật khẩu
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại"));

        String otp = OtpUtil.generateOTP();
        user.setOtp(otp);
        user.setOtpExpires(new Date(System.currentTimeMillis() + 5 * 60 * 1000)); // 5 phút
        userRepository.save(user);

        mailService.sendMail(
                email,
                "OTP Reset mật khẩu",
                "Mã OTP của bạn là: " + otp
        );
    }

    // Cập nhật lại mật khẩu (Lấy lại mật khẩu)
    public void resetPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại"));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setOtp("");
        user.setOtpExpires(new Date(0));
        userRepository.save(user);
    }

    // Cập nhật mật khẩu
    public void updatePassword(String id, String oldPassword, String newPassword, String confirmPassword) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // kiểm tra mật khẩu cũ
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Mật khẩu cũ không đúng");
        }

        // cập nhật mật khẩu mới
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public User updateUser(String id, Map<String, String> form, MultipartFile avatar, String authUserId) {
        if (authUserId == null || !id.equals(authUserId)) {
            throw new RuntimeException("Bạn không có quyền truy cập thông tin này");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        // Cập nhật các field text (giống updateData ở Express)
        if (form.containsKey("fullname")) {
            user.setFullname(form.get("fullname"));
        }
        if (form.containsKey("sex")) {
            try {
                user.setSex(Integer.parseInt(form.get("sex"))); // 0|1
            } catch (NumberFormatException ignored) {}
        }
        if (form.containsKey("dob")) {
            // chấp nhận "yyyy-MM-dd" (giống date input trên web)
            String dobStr = form.get("dob");
            try {
                Date dob = new SimpleDateFormat("yyyy-MM-dd").parse(dobStr);
                user.setDob(dob);
            } catch (ParseException ignored) {}
        }

        // Upload avatar nếu có file
        if (avatar != null && !avatar.isEmpty()) {
            String url = cloudinaryService.uploadImage(avatar, "Avatar");
            user.setAvatar(url);
        }

        if (form.containsKey("level")) {
            UserOnboarding onboarding = userOnboardingRepository.findByUserId(id)
                    .orElse(UserOnboarding.builder().userId(id).build());
            onboarding.setLevel(form.get("level"));
            userOnboardingRepository.save(onboarding);
        }


        return userRepository.save(user);
    }

    // Service: ADMIN tạo user
    public User createUserByAdmin(Map<String, Object> userData) {
        String email = userData.get("email") != null ? userData.get("email").toString() : null;
        String rawPassword = userData.get("password") != null ? userData.get("password").toString() : null;
        String roleStr = userData.get("role") != null ? userData.get("role").toString() : "USER";
        String fullname = userData.get("fullname") != null ? userData.get("fullname").toString() : "New User";

        if (email == null || rawPassword == null) {
            throw new RuntimeException("Email hoặc mật khẩu không hợp lệ");
        }

        // Email đã tồn tại?
        userRepository.findByEmail(email).ifPresent(u -> {
            throw new RuntimeException("Email đã tồn tại trong hệ thống");
        });

        Role role;
        try {
            role = Role.valueOf(roleStr.toUpperCase()); // "USER" | "ADMIN"
        } catch (IllegalArgumentException ex) {
            role = Role.USER; // fallback giống Express (không validate phức tạp)
        }

        // Hash password
        String hashed = passwordEncoder.encode(rawPassword);

        // Tạo user: isVerified = true, otp rỗng, otpExpires null (đúng tinh thần Express)
        User newUser = User.builder()
                .email(email)
                .password(hashed)
                .fullname(fullname)
                .role(role)
                .isVerified(true)
                .otp("")           // trống
                .otpExpires(new Date(0))  // không set hạn OTP
                .subscription(
                        // giữ default NONE (nếu entity đã set @Builder.Default thì có thể bỏ block này)
                        com.sc.scifunapi.entity.Subscription.builder()
                                .status(SubscriptionStatus.NONE)
                                .build()
                )
                .build();

        return userRepository.save(newUser);
    }

    // Xóa người dùng
    public void deleteUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new RuntimeException("ID người dùng không hợp lệ");
        }

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        userRepository.delete(user);
    }

    // Lấy chi tiết
    public Map<String, Object>  getInfoUser(String id, String authUserId) {
        // Chỉ cho phép xem chính mình
        if (authUserId == null || !id.equals(authUserId)) {
            throw new RuntimeException("Bạn không có quyền truy cập thông tin này");
        }
        UserStatusResult userStatusResult = getStatus(authUserId);

        var u = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        Map<String, Object> res = new HashMap<>();
        res.put("id", u.getId());
        res.put("email", u.getEmail());
        res.put("fullname", u.getFullname());
        res.put("avatar", u.getAvatar());
        res.put("role", u.getRole());
        res.put("dob", u.getDob());
        res.put("sex", u.getSex());
        res.put("subscription", u.getSubscription());
        res.put("isGuest", userStatusResult.isGuest);
        res.put("expiredAt", u.getExpiredAt() != null ? u.getExpiredAt() : null);
        res.put("level", getUserLevel(id)); // dùng lại hàm đã có
        return res;
    }

    public UserStatusResult getStatus(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        boolean isGuest = user.getEmail() != null
                && user.getEmail().endsWith("@guest.local");
        return new UserStatusResult(isGuest);
    }

    public User findById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
    }

    // Lấy danh sách người dùng có phân trang
    public Map<String, Object> getUserList(Integer page, Integer limit, String search) {
        Query query = new Query();

        // tìm theo email hoặc fullname
        if (search != null && !search.isBlank()) {
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("email").regex(search, "i"),
                    Criteria.where("fullname").regex(search, "i")
            ));
        }

        // ẩn các trường nhạy cảm
        query.fields().exclude("password").exclude("otp").exclude("otpExpires");

        // sắp xếp theo id mới nhất
        query.with(Sort.by(Sort.Direction.DESC, "_id"));

        Map<String, Object> result = new HashMap<>();

        // Nếu không có page/limit → lấy tất cả
        if (page == null || limit == null) {
            List<User> users = mongoTemplate.find(query, User.class);
            result.put("users", users);
            result.put("total", users.size());
            result.put("page", 1);
            result.put("limit", users.size());
            result.put("totalPages", 1);
            return result;
        }

        // Có page/limit → phân trang
        int skip = Math.max(0, (page - 1) * limit);

        Query pagedQuery = query.limit(limit).skip(skip);
        List<User> users = mongoTemplate.find(pagedQuery, User.class);

        // Đếm tổng kết quả
        long total = mongoTemplate.count(query, User.class);

        result.put("users", users);
        result.put("total", total);
        result.put("page", page);
        result.put("limit", limit);
        result.put("totalPages", (int) Math.ceil(total / (double) limit));
        return result;
    }

    public Map<String, Object> refreshGuestToken(String userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        // Guest đã hết hạn tài khoản (expiredAt) thì không refresh
        if (user.getExpiredAt() != null && user.getExpiredAt().before(new Date())) {
            userRepository.delete(user);
            throw new RuntimeException("Phiên dùng thử đã hết hạn, vui lòng đăng ký tài khoản");
        }

        // Gia hạn thêm 30 ngày
        user.setExpiredAt(new Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000));
        userRepository.save(user);

        String newToken = jwtUtil.generateGuestToken(user.getId(), user.getEmail());
        return Map.of("token", newToken);
    }

    public Map<String, Object> createGuest() {
        String guestId = UUID.randomUUID().toString();

        User guest = User.builder()
                .id(guestId)
                .email(guestId + "@guest.local")
                .fullname("Guest User")
                .role(Role.USER)
                .isVerified(true)
                .expiredAt(new Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000))
                .build();

        userRepository.save(guest);

        String token = jwtUtil.generateGuestToken(guest.getId(), guest.getEmail());
        boolean isFirstLogin = guest.isFirstLogin();
        markNotFirstLogin(guest.getId());
        return Map.of("token", token, "userId", guest.getId(), "isFisrtLogin", isFirstLogin);
    }

    public void deleteGuestUser(String userId) {
        userProgressRepository.deleteByUserId(userId);
        userOnboardingRepository.deleteByUserId(userId);
        submissionRepository.deleteByUserId(userId);
        resultRepository.deleteByUserId(userId);
        leaderboardRepository.deleteByUserId(userId);
        favoriteQuizRepository.deleteByUser(userId);
        userRepository.deleteById(userId);
    }

    public Map<String, Object> convertGuestToUser(String userId, RegisterRequest req) {
        // Kiểm tra email đã tồn tại chưa (tránh trùng với user thật khác)
        userRepository.findByEmail(req.getEmail()).ifPresent(existing -> {
            if (!existing.getId().equals(userId)) {
                throw new RuntimeException("Email đã được sử dụng, vui lòng dùng email khác");
            }
        });

        User guest = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên khách"));

        // Cập nhật thông tin
        guest.setEmail(req.getEmail());
        guest.setFullname(req.getFullname() != null ? req.getFullname() : "New User");
        guest.setPassword(passwordEncoder.encode(req.getPassword()));
        guest.setOtp(OtpUtil.generateOTP());
        guest.setOtpExpires(new Date(System.currentTimeMillis() + 5 * 60 * 1000));
        guest.setVerified(false);       // chờ xác thực OTP
        guest.setExpiredAt(null);       // ← convert thành user thật

        userRepository.save(guest);

        // Gửi OTP
        mailService.sendMail(
                guest.getEmail(),
                "OTP xác thực đăng ký",
                "Mã OTP của bạn là: " + guest.getOtp()
        );

        Map<String, Object> res = new HashMap<>();
        res.put("id", guest.getId());
        res.put("email", guest.getEmail());
        res.put("fullname", guest.getFullname());
        res.put("isVerified", guest.isVerified());
        res.put("role", guest.getRole());
        return res;
    }
}
