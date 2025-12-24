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
import org.cloudinary.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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

    @Value("${scifun.client-url}")
    private String clientUrl;

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

    public int queryReturnCode(String appTransId) {
        try {
            // 1. Tạo MAC
            String data = appId + "|" + appTransId + "|" + key1;
            String mac = hmacSha256(data, key1);

            // 2. Tạo JSON payload
            JSONObject payload = new JSONObject();
            payload.put("app_id", Integer.parseInt(appId));
            payload.put("app_trans_id", appTransId);
            payload.put("mac", mac);

            // 3. Gọi API
            URL url = new URL("https://sb-openapi.zalopay.vn/v2/query");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // 4. Gửi request
            OutputStream os = conn.getOutputStream();
            os.write(payload.toString().getBytes("UTF-8"));
            os.flush();
            os.close();

            // 5. Đọc response
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "UTF-8"));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // 6. Parse JSON và lấy return_code
                JSONObject jsonResponse = new JSONObject(response.toString());
                int returnCode = jsonResponse.getInt("return_code");

                return returnCode;
            } else {
                System.err.println("HTTP Error: " + responseCode);
                return -1; // Hoặc throw exception
            }

        } catch (Exception e) {
            e.printStackTrace();
            return -1; // Hoặc throw exception
        }
    }

    // ========== 1. Tạo order ZaloPay ==========

    public ZlpCreateOrderResponse createOrder(double amount, String userId) throws Exception {
        String appTransId = genAppTransId();
        long appTime = System.currentTimeMillis();
        String appUser = (userId != null && !userId.isBlank()) ? userId : "guest";

        String embedData = objectMapper.writeValueAsString(
                Map.of("redirecturl", clientUrl)
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
