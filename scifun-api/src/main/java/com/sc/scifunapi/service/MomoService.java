package com.sc.scifunapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sc.scifunapi.dto.momo.MomoCreateOrderResponse;
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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MomoService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Value("${momo.partner-code}")   private String partnerCode;
    @Value("${momo.access-key}")     private String accessKey;
    @Value("${momo.secret-key}")     private String secretKey;
    @Value("${momo.endpoint}")       private String endpoint;
    @Value("${momo.redirect-url}")   private String redirectUrl;  // deep link Flutter
    @Value("${momo.ipn-url}")        private String ipnUrl;       // webhook server

    // ─── HMAC-SHA256 ───────────────────────────────────────────────
    public String hmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : raw) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("HMAC error", e);
        }
    }

    // ─── Sinh orderId & requestId ──────────────────────────────────
    private String genOrderId() {
        return partnerCode + System.currentTimeMillis();
    }

    // ─── 1. Tạo order MoMo ────────────────────────────────────────
    public MomoCreateOrderResponse createOrder(double amount, String userId) throws Exception {

        String orderId   = genOrderId();
        String requestId = orderId;                      // thường dùng chung
        String orderInfo = "Thanh toán gói PRO Scifun";
        String extraData = "";                           // base64, để trống cũng được
        String requestType = "captureWallet";            // QR + deeplink


        // Chuỗi ký — đúng thứ tự MoMo yêu cầu
        String rawHash = "accessKey="   + accessKey
                + "&amount="      + (long) amount
                + "&extraData="   + extraData
                + "&ipnUrl="      + ipnUrl
                + "&orderId="     + orderId
                + "&orderInfo="   + orderInfo
                + "&partnerCode=" + partnerCode
                + "&redirectUrl=" + redirectUrl
                + "&requestId="   + requestId
                + "&requestType=" + "captureWallet";

        String signature = hmacSha256(rawHash, secretKey);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("partnerCode",  partnerCode);
        body.put("accessKey",    accessKey);
        body.put("requestId",    requestId);
        body.put("amount",       (long) amount);
        body.put("orderId",      orderId);
        body.put("orderInfo",    orderInfo);
        body.put("redirectUrl",  redirectUrl);
        body.put("ipnUrl",       ipnUrl);
        body.put("extraData",    extraData);
        body.put("requestType",  "captureWallet");
        body.put("lang",         "vi");
        body.put("signature",    signature);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<MomoCreateOrderResponse> resp = restTemplate.postForEntity(
                endpoint,
                new HttpEntity<>(body, headers),
                MomoCreateOrderResponse.class
        );

        MomoCreateOrderResponse res = resp.getBody();
        if (res == null || res.getResultCode() != 0) {
            throw new RuntimeException("MoMo create order failed: " +
                    (res != null ? res.getMessage() : "null response"));
        }

        return res;
    }

    // ─── 2. Áp dụng thanh toán khi IPN về ────────────────────────
    public PaymentResult applyPaymentIfSuccess(String orderId,
                                               int resultCode,
                                               int durationDays) {
        // MoMo: resultCode == 0 là thành công
        if (resultCode != 0) return PaymentResult.IGNORED_FAILED;

        Order order = orderRepository
                .findByProviderRefAndProvider(orderId, Provider.MOMO)
                .orElse(null);

        if (order == null)                             return PaymentResult.NOT_FOUND;
        if (order.getStatus() == OrderStatus.PAID)     return PaymentResult.ALREADY_PAID;

        // Cập nhật order
        order.setStatus(OrderStatus.PAID);
        order.setPeriod(durationDays >= 30 ? Period.month : Period.week);
        orderRepository.save(order);

        // Cập nhật subscription user
        User user = userRepository.findById(order.getUserId()).orElse(null);
        if (user != null) {
            Subscription sub = Optional.ofNullable(user.getSubscription())
                    .orElse(new Subscription());

            Instant now  = Instant.now();
            Instant base = (sub.getCurrentPeriodEnd() != null &&
                    sub.getCurrentPeriodEnd().toInstant().isAfter(now))
                    ? sub.getCurrentPeriodEnd().toInstant() : now;

            sub.setStatus(SubscriptionStatus.ACTIVE);
            sub.setTier(SubscriptionTier.PRO);
            sub.setCurrentPeriodEnd(Date.from(base.plus(durationDays, ChronoUnit.DAYS)));
            sub.setProvider(PaymentProvider.MOMO);

            user.setSubscription(sub);
            userRepository.save(user);
        }

        return PaymentResult.PAID;
    }
}