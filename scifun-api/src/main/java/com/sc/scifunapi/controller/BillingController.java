package com.sc.scifunapi.controller;

import com.sc.scifunapi.dto.zalo.ZlpCreateOrderResponse;
import com.sc.scifunapi.entity.Order;
import com.sc.scifunapi.enums.Currency;
import com.sc.scifunapi.enums.OrderStatus;
import com.sc.scifunapi.enums.OrderType;
import com.sc.scifunapi.enums.PaymentResult;
import com.sc.scifunapi.enums.Period;
import com.sc.scifunapi.enums.PlanTier;
import com.sc.scifunapi.enums.Provider;
import com.sc.scifunapi.repository.OrderRepository;
import com.sc.scifunapi.service.ZaloPayService;
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

    private final ZaloPayService zaloPayService;
    private final OrderRepository orderRepository;

    @Value("${zalopay.app-id}")
    private Integer zaloAppId;

    // ================== TẠO ĐƠN ZALOPAY ==================
    @PostMapping("/checkout")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<?> createCheckout(@RequestBody Map<String, Object> body) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userId = (auth != null && auth.getDetails() != null)
                    ? auth.getDetails().toString()
                    : "guest";

            double planPrice = 0.0;
            if (body.get("price") != null) {
                planPrice = Double.parseDouble(body.get("price").toString());
            }

            // Gọi service tạo order ZaloPay
            ZlpCreateOrderResponse zlpRes = zaloPayService.createOrder(planPrice, userId);

            // check returnCode
            if (zlpRes.getReturnCode() != 1) {
                return ResponseEntity.badRequest().body(
                        Map.of(
                                "status", 400,
                                "message", "Tạo thanh toán ZaloPay thất bại",
                                "return_message", zlpRes.getReturnMessage()
                        )
                );
            }

            // Lưu Order PENDING
            Order order = Order.builder()
                    .userId(userId)
                    .type(OrderType.SUBSCRIPTION)
                    .total(planPrice)
                    .currency(Currency.VND)
                    .provider(Provider.ZALOPAY)
                    .providerRef(zlpRes.getAppTransId())
                    .status(OrderStatus.PENDING)
                    .planTier(PlanTier.PRO)
                    .period(Period.month)
                    .build();

            orderRepository.save(order);

            // Tạo response map (cho phép value null)
            Map<String, Object> resp = new java.util.HashMap<>();
            resp.put("provider", "ZALOPAY");
            resp.put("payUrl", zlpRes.getOrderUrl());
            resp.put("appTransId", zlpRes.getAppTransId());
            resp.put("orderId", order.getId());
            resp.put("mac", zlpRes.getMac());

            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(
                    Map.of(
                            "status", 500,
                            "message", e.getMessage() != null ? e.getMessage() : e.getClass().getName()
                    )
            );
        }
    }


    // ================== VERIFY PAYMENT ==================

    // Body request verify
    public static class VerifyPaymentRequest {
        public String appTransId;
        public Integer returnCode;
        public Integer durationDays;
    }

    @PostMapping("/zalopay/verifyPayment")
    public ResponseEntity<?> verifyPayment(@RequestBody VerifyPaymentRequest req) {
        try {
            if (req.appTransId == null || req.appTransId.isBlank()) {
                return ResponseEntity.badRequest().body(
                        Map.of("status", 400, "message", "Thiếu appTransId")
                );
            }

            int returnCode = (req.returnCode != null) ? req.returnCode : 1;
            int durationDays = (req.durationDays != null) ? req.durationDays : 30;

            PaymentResult result = zaloPayService.applyPaymentIfSuccess(
                    req.appTransId,
                    returnCode,
                    durationDays
            );

            return switch (result) {
                case PAID -> ResponseEntity.ok(
                        Map.of("status", 200,
                                "message", "Thanh toán thành công, user đã được nâng cấp PRO"));
                case ALREADY_PAID -> ResponseEntity.ok(
                        Map.of("status", 200,
                                "message", "Đơn hàng này đã được thanh toán trước đó"));
                case NOT_FOUND -> ResponseEntity.status(404).body(
                        Map.of("status", 404,
                                "message", "Không tìm thấy đơn hàng"));
                case IGNORED_FAILED -> ResponseEntity.ok(
                        Map.of("status", 200,
                                "message", "Chưa hoàn thành thanh toán"));
            };

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(
                    Map.of(
                            "status", 500,
                            "message", "Lỗi xác nhận thanh toán",
                            "error", e.getMessage()
                    )
            );
        }
    }
}
