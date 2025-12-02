package com.sc.scifunapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sc.scifunapi.dto.zalo.ZlpCreateOrderResponse;
import com.sc.scifunapi.entity.Order;
import com.sc.scifunapi.entity.Subscription;
import com.sc.scifunapi.entity.User;
import com.sc.scifunapi.enums.*;
import com.sc.scifunapi.repository.OrderRepository;
import com.sc.scifunapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ZaloPayService {

    private final RestTemplate restTemplate;     // nhớ cấu hình bean RestTemplate
    private final ObjectMapper objectMapper;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Value("${zalopay.app-id}")
    private String appId;

    @Value("${zalopay.key1}")
    private String key1;

    @Value("${zalopay.create-endpoint}")
    private String createEndpoint;

    // ========== Helper ==========

    private String hmacSha256(String data, String key) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(
                    key.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            ));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : raw) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error signing HMAC", e);
        }
    }

    private String genAppTransId() {
        // YYMMDD_random6
        java.time.format.DateTimeFormatter f =
                java.time.format.DateTimeFormatter.ofPattern("yyMMdd");
        String yymmdd = java.time.LocalDate.now().format(f);
        int rand = (int) (Math.random() * 1_000_000);
        return yymmdd + "_" + rand;
    }

    // ========== 1. Tạo order ZaloPay ==========

    public ZlpCreateOrderResponse createOrder(double amount, String userId) throws Exception {
        String appTransId = genAppTransId();
        long appTime = System.currentTimeMillis();
        String appUser = (userId != null && !userId.isBlank()) ? userId : "guest";

        String embedData = objectMapper.writeValueAsString(
                Map.of("redirecturl", "https://your-client-url.com/premium")
        );

        String item = "[]";
        String description = "Thanh toán gói PRO Scifun";

        String macInput = String.join("|",
                appId,
                appTransId,
                appUser,
                String.valueOf((long) amount),
                String.valueOf(appTime),
                embedData,
                item
        );

        String mac = hmacSha256(macInput, key1);

        Map<String, Object> body = Map.of(
                "app_id", Integer.parseInt(appId),
                "app_user", appUser,
                "app_time", appTime,
                "amount", (long) amount,
                "app_trans_id", appTransId,
                "embed_data", embedData,
                "item", item,
                "description", description,
                "mac", mac
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> resp = restTemplate.postForEntity(
                createEndpoint,
                entity,
                String.class
        );

        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException(
                    "ZaloPay create failed: " + resp.getStatusCodeValue() + " " + resp.getBody()
            );
        }

        ZlpCreateOrderResponse res =
                objectMapper.readValue(resp.getBody(), ZlpCreateOrderResponse.class);

        // gắn thêm appTransId mình sinh ra
        res.setAppTransId(appTransId);

        // Có thể override mac nếu muốn tự tính lại
        String macCheck = hmacSha256(appId + "|" + appTransId + "|" + key1, key1);
        res.setMac(macCheck);

        return res;
    }

    // ========== 2. Áp dụng kết quả thanh toán ==========

    public PaymentResult applyPaymentIfSuccess(String appTransId,
                                               int returnCode,
                                               int durationDays) {

        // chỉ xử lý khi ZaloPay báo thành công
        if (returnCode != 1) {
            return PaymentResult.IGNORED_FAILED;
        }

        Order order = orderRepository
                .findByProviderRefAndProvider(appTransId, Provider.ZALOPAY)
                .orElse(null);

        if (order == null) {
            return PaymentResult.NOT_FOUND;
        }

        if (order.getStatus() == OrderStatus.PAID) {
            return PaymentResult.ALREADY_PAID;
        }

        // cập nhật order
        order.setStatus(OrderStatus.PAID);
        order.setPeriod(durationDays >= 30 ? Period.month : Period.week);
        orderRepository.save(order);

        // cập nhật User.subscription
        User user = userRepository.findById(order.getUserId()).orElse(null);
        if (user != null) {
            Subscription sub = user.getSubscription();
            if (sub == null) {
                sub = new Subscription();
            }

            Instant now = Instant.now();
            Instant base = (sub.getCurrentPeriodEnd() != null &&
                    sub.getCurrentPeriodEnd().toInstant().isAfter(now))
                    ? sub.getCurrentPeriodEnd().toInstant()
                    : now;

            Date newEnd = Date.from(base.plus(durationDays, ChronoUnit.DAYS));

            sub.setStatus(SubscriptionStatus.ACTIVE);
            sub.setTier(SubscriptionTier.PRO);
            sub.setCurrentPeriodEnd(newEnd);
            sub.setProvider(PaymentProvider.ZALOPAY);

            user.setSubscription(sub);
            userRepository.save(user);
        }

        return PaymentResult.PAID;
    }
}
