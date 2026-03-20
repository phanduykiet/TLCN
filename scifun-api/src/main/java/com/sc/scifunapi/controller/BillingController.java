package com.sc.scifunapi.controller;

import com.sc.scifunapi.dto.momo.MomoCreateOrderResponse;
import com.sc.scifunapi.entity.Order;
import com.sc.scifunapi.enums.*;
import com.sc.scifunapi.repository.OrderRepository;
import com.sc.scifunapi.service.MomoService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BillingController {

    private final MomoService momoService;
    private final OrderRepository orderRepository;

    @Value("${momo.access-key}")      // ← THÊM field này
    private String momoAccessKey;

    @Value("${momo.secret-key}")
    private String momoSecretKey;

    // ── 1. Tạo đơn MoMo ──────────────────────────────────────────
    @PostMapping("/checkout")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<?> createCheckout(@RequestBody Map<String, Object> body) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userId = (auth != null && auth.getDetails() != null)
                    ? auth.getDetails().toString() : "guest";

            double planPrice  = body.get("price") != null
                    ? Double.parseDouble(body.get("price").toString()) : 0;
            int durationDays  = body.get("durationDays") != null
                    ? Integer.parseInt(body.get("durationDays").toString()) : 30;

            MomoCreateOrderResponse momoRes = momoService.createOrder(planPrice, userId);

            // Lưu order PENDING
            Order order = Order.builder()
                    .userId(userId)
                    .type(OrderType.SUBSCRIPTION)
                    .total(planPrice)
                    .currency(Currency.VND)
                    .provider(Provider.MOMO)
                    .providerRef(momoRes.getOrderId())   // orderId MoMo sinh ra
                    .status(OrderStatus.PENDING)
                    .planTier(PlanTier.PRO)
                    .period(Period.month)
                    .build();
            orderRepository.save(order);

            Map<String, Object> resp = new java.util.HashMap<>();
            resp.put("provider",     "MOMO");
            resp.put("payUrl",       momoRes.getPayUrl());     // mở webview/browser
            resp.put("deeplink",     momoRes.getDeeplink());   // mở thẳng app MoMo
            resp.put("qrCodeUrl",    momoRes.getQrCodeUrl());  // hiển thị QR
            resp.put("orderId",      momoRes.getOrderId());
            resp.put("durationDays", durationDays);

            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(
                    Map.of("status", 500, "message",
                            e.getMessage() != null ? e.getMessage() : e.getClass().getName()));
        }
    }

    // ── 2. IPN — MoMo gọi về tự động sau khi thanh toán ─────────
    // KHÔNG cần auth vì MoMo server gọi thẳng vào đây
    @PostMapping("/momo/ipn")
    public ResponseEntity<?> momoIpn(@RequestBody Map<String, Object> payload) {
        try {
            String orderId    = (String) payload.get("orderId");
            String requestId  = (String) payload.get("requestId");
            int    resultCode = Integer.parseInt(payload.get("resultCode").toString()); // ← parse an toàn hơn
            long   amount     = Long.parseLong(payload.get("amount").toString());
            String signature  = (String) payload.get("signature");

            // Log để debug
            System.out.println("=== MOMO IPN ===");
            System.out.println("orderId: " + orderId);
            System.out.println("resultCode: " + resultCode);
            System.out.println("signature nhận: " + signature);

            // Verify chữ ký — accessKey đã có đúng giá trị
            String rawHash = "accessKey="    + momoAccessKey   // ← FIX: không còn trống
                    + "&amount="       + amount
                    + "&extraData="    + payload.getOrDefault("extraData", "")
                    + "&message="      + payload.getOrDefault("message", "")
                    + "&orderId="      + orderId
                    + "&orderInfo="    + payload.getOrDefault("orderInfo", "")
                    + "&orderType="    + payload.getOrDefault("orderType", "")
                    + "&partnerCode="  + payload.getOrDefault("partnerCode", "")
                    + "&payType="      + payload.getOrDefault("payType", "")
                    + "&requestId="    + requestId
                    + "&responseTime=" + payload.getOrDefault("responseTime", "")
                    + "&resultCode="   + resultCode
                    + "&transId="      + payload.getOrDefault("transId", "");

            System.out.println("rawHash: " + rawHash);

            String expectedSig = momoService.hmacSha256(rawHash, momoSecretKey);
            System.out.println("signature expected: " + expectedSig);

            if (!expectedSig.equals(signature)) {
                System.out.println("=== SIGNATURE KHÔNG KHỚP ===");
                return ResponseEntity.ok(Map.of("resultCode", 99, "message", "invalid signature"));
            }

            momoService.applyPaymentIfSuccess(orderId, resultCode, 30);

            return ResponseEntity.ok(Map.of("resultCode", 0, "message", "success"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(Map.of("resultCode", 99, "message", e.getMessage()));
        }
    }
}