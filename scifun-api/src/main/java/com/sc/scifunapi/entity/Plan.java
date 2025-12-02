package com.sc.scifunapi.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "plans")
public class Plan {

    @Id
    private String id;

    @Indexed(unique = true)
    private String name; // "Gói Tuần", "Gói Tháng"

    private double price; // 99000

    private int durationDays; // 7, 30

    @CreatedDate
    private Date createdAt;

    @LastModifiedDate
    private Date updatedAt;
}
