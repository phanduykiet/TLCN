package com.sc.scifunapi.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.sc.scifunapi.service.OrderService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders") 
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin-get")
    public ResponseEntity<Map<String, Object>> adminGetOrders(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            @RequestParam(name = "userId", required = false) String userId) {

        try {
            Map<String, Object> result = orderService.getOrders(userId, page, limit);

            return ResponseEntity.ok(
                    Map.of(
                            "status", 200,
                            "message", "Lấy danh sách thành công",
                            "data", result
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "status", 400,
                            "message", e.getMessage()
                    )
            );
        }
    }

    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/user-get")
    public ResponseEntity<Map<String, Object>> userGetOrders(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "10") int limit) {
        try {
            // userId đã set ở JwtAuthFilter: authentication.setDetails(userId)
            String authUserId = (String) SecurityContextHolder.getContext()
                    .getAuthentication().getDetails();

            Map<String, Object> result = orderService.getOrders(authUserId, page, limit);

            return ResponseEntity.ok(
                    Map.of(
                            "status", 200,
                            "message", "Lấy danh sách thành công",
                            "data", result
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "status", 400,
                            "message", e.getMessage()
                    )
            );
        }
    }


}