package com.sc.scifunapi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class MailService {

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.mail.fromName}")
    private String fromName;

    @Value("${app.brevo.apiKey}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Async("mailExecutor")
    public void sendMail(String to, String subject, String text) {
        int maxRetries = 2;
        int attempt = 0;

        while (attempt <= maxRetries) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("api-key", apiKey);
                headers.set("accept", "application/json");

                Map<String, Object> sender = new HashMap<>();
                sender.put("name", fromName);
                sender.put("email", from);

                Map<String, Object> recipient = new HashMap<>();
                recipient.put("email", to);

                Map<String, Object> body = new HashMap<>();
                body.put("sender", sender);
                body.put("to", new Object[]{recipient});
                body.put("subject", subject);
                body.put("textContent", text);

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

                ResponseEntity<String> response = restTemplate.postForEntity(
                        "https://api.brevo.com/v3/smtp/email", request, String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("Mail sent successfully to {}", to);
                    return;
                } else {
                    throw new RuntimeException("Brevo send failed: " + response.getStatusCode() + " " + response.getBody());
                }

            } catch (Exception e) {
                attempt++;
                log.warn("Brevo send attempt {} failed: {}", attempt, e.getMessage());
                if (attempt > maxRetries) {
                    log.error("Brevo send failed after {} attempts", attempt, e);
                    return;
                }
            }
        }
    }
}