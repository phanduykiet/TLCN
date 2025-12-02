package com.sc.scifunapi.service;

import com.sc.scifunapi.entity.Plan;
import com.sc.scifunapi.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;

    public Plan createPlan(String name, double price, int durationDays) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên gói không được để trống");
        }
        if (price < 0) {
            throw new IllegalArgumentException("Giá không hợp lệ");
        }
        if (durationDays < 0) {
            throw new IllegalArgumentException("Số ngày không hợp lệ");
        }

        planRepository.findByName(name).ifPresent(p -> {
            throw new RuntimeException("Tên gói đã tồn tại");
        });

        Plan plan = Plan.builder()
                .name(name.trim())
                .price(price)
                .durationDays(durationDays)
                .build();

        return planRepository.save(plan);
    }

    public Plan updatePlan(String id, Map<String, Object> body) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói"));

        boolean hasUpdate = false;

        // name
        if (body.get("name") instanceof String nameRaw) {
            String name = nameRaw.trim();
            if (!name.isEmpty() && !name.equals(plan.getName())) {
                // kiểm tra trùng tên (trừ chính nó)
                if (planRepository.existsByNameAndIdNot(name, id)) {
                    throw new IllegalStateException("Tên gói đã tồn tại");
                }
                plan.setName(name);
                hasUpdate = true;
            }
        }

        // price
        if (body.get("price") != null) {
            try {
                double price = Double.parseDouble(body.get("price").toString());
                plan.setPrice(price);
                hasUpdate = true;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Giá không hợp lệ");
            }
        }

        // durationDays
        if (body.get("durationDays") != null) {
            try {
                int durationDays = Integer.parseInt(body.get("durationDays").toString());
                plan.setDurationDays(durationDays);
                hasUpdate = true;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("durationDays không hợp lệ");
            }
        }

        if (!hasUpdate) {
            throw new IllegalArgumentException("Không có dữ liệu cập nhật");
        }

        return planRepository.save(plan);
    }

    public void deletePlan(String id) {
        // validate ObjectId giống Types.ObjectId.isValid bên Node
        if (id == null || !ObjectId.isValid(id)) {
            throw new IllegalArgumentException("ID không hợp lệ");
        }

        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy gói"));

        planRepository.delete(plan);
    }

    public List<Plan> listPlans() {
        return planRepository.findAll(Sort.by(Sort.Direction.ASC, "durationDays"));
    }

    // 🔹 Lấy chi tiết một Plan theo id
    public Plan getPlanById(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("ID không hợp lệ");
        }

        return planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy gói"));
    }
}
