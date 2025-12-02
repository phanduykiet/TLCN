package com.sc.scifunapi.repository;

import com.sc.scifunapi.entity.Plan;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlanRepository extends MongoRepository<Plan, String> {

    Optional<Plan> findByName(String name);
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, String id);

}
