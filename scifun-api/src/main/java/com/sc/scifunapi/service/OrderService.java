package com.sc.scifunapi.service;

import com.sc.scifunapi.entity.Order;
import com.sc.scifunapi.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    public Map<String, Object> getOrders(String userId, int page, int limit) {

        if (page < 1) page = 1;
        if (limit < 1) limit = 10;

        Pageable pageable = PageRequest.of(
                page - 1,
                limit,
                Sort.by(Sort.Direction.DESC, "createdAt") // sort cho đẹp
        );

        Page<Order> orderPage;

        if (!StringUtils.hasText(userId)) {
            orderPage = orderRepository.findAll(pageable);
        } else {
            orderPage = orderRepository.findByUserId(userId, pageable);
        }

        List<Order> data = orderPage.getContent();

        Map<String, Object> res = new HashMap<>();
        res.put("page", page);
        res.put("limit", limit);
        res.put("total", orderPage.getTotalElements());
        res.put("totalPages", orderPage.getTotalPages());
        res.put("data", data);

        return res;
    }
}