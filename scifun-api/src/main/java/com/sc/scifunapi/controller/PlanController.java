package com.sc.scifunapi.controller;

import com.sc.scifunapi.dto.plan.CreatePlanRequest;
import com.sc.scifunapi.entity.Plan;
import com.sc.scifunapi.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createPlan(@RequestBody CreatePlanRequest req) {
        try {
            if (req.getName() == null ||
                    req.getPrice() == null ||
                    req.getDurationDays() == null) {

                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "status", 400,
                                "success", false,
                                "message", "Thiếu hoặc sai dữ liệu"
                        ));
            }

            Plan plan = planService.createPlan(
                    req.getName(),
                    req.getPrice(),
                    req.getDurationDays()
            );

            return ResponseEntity.status(201).body(
                    Map.of(
                            "status", 201,
                            "success", true,
                            "data", plan
                    )
            );

        } catch (RuntimeException e) {
            return ResponseEntity.status(409).body(
                    Map.of(
                            "status", 409,
                            "success", false,
                            "message", e.getMessage()
                    )
            );

        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of(
                            "status", 500,
                            "success", false,
                            "message", e.getMessage()
                    )
            );
        }
    }

    // ✅ PUT /plans/update/{id}
    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updatePlan(
            @PathVariable String id,
            @RequestBody Map<String, Object> body
    ) {
        try {
            Plan updated = planService.updatePlan(id, body);

            return ResponseEntity.ok(
                    Map.of(
                            "status", 200,
                            "success", true,
                            "data", updated
                    )
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "status", 400,
                            "success", false,
                            "message", e.getMessage()
                    )
            );
        } catch (IllegalStateException e) { // trùng tên gói
            return ResponseEntity.status(409).body(
                    Map.of(
                            "status", 409,
                            "success", false,
                            "message", e.getMessage()
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of(
                            "status", 500,
                            "success", false,
                            "message", e.getMessage()
                    )
            );
        }
    }

    // ✅ DELETE /plans/delete/{id}
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deletePlan(@PathVariable String id) {
        try {
            planService.deletePlan(id);

            return ResponseEntity.ok(
                    Map.of(
                            "status", 200,
                            "success", true,
                            "message", "Đã xóa gói"
                    )
            );

        } catch (IllegalArgumentException e) { // ID không hợp lệ
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "status", 400,
                            "success", false,
                            "message", e.getMessage()
                    )
            );
        } catch (IllegalStateException e) { // Không tìm thấy gói
            return ResponseEntity.status(404).body(
                    Map.of(
                            "status", 404,
                            "success", false,
                            "message", e.getMessage()
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of(
                            "status", 500,
                            "success", false,
                            "message", e.getMessage()
                    )
            );
        }
    }

    // GET /api/v1/plans/list
    @GetMapping("/list")
    public ResponseEntity<?> listPlans() {
        try {
            List<Plan> plans = planService.listPlans();
            return ResponseEntity.ok(
                    Map.of(
                            "status", 200,
                            "success", true,
                            "data", plans
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of(
                            "status", 500,
                            "success", false,
                            "message", e.getMessage()
                    )
            );
        }
    }

    // 🔹 GET /api/v1/plans/getId/{id}
    @GetMapping("/getId/{id}")
    public ResponseEntity<?> getPlan(@PathVariable String id) {
        try {
            Plan plan = planService.getPlanById(id);

            return ResponseEntity.ok(
                    Map.of(
                            "status", 200,
                            "success", true,
                            "data", plan
                    )
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "status", 400,
                            "success", false,
                            "message", e.getMessage()
                    )
            );
        } catch (RuntimeException e) {
            // "Không tìm thấy gói"
            return ResponseEntity.status(404).body(
                    Map.of(
                            "status", 404,
                            "success", false,
                            "message", e.getMessage()
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of(
                            "status", 500,
                            "success", false,
                            "message", e.getMessage()
                    )
            );
        }
    }
}
