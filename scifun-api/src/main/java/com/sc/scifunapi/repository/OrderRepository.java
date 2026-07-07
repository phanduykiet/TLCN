package com.sc.scifunapi.repository;

import com.sc.scifunapi.entity.Order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {
    Optional<Order> findByProviderRefAndProvider(String providerRef, Enum provider);

    Page<Order> findByUserId(String userId, Pageable pageable);

    // OrderRepository.java
    Optional<Order> findByProviderRef(String providerRef);

    List<Order> findTop5ByOrderByCreatedAtDesc();
}
