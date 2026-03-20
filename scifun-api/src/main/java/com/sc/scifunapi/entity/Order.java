package com.sc.scifunapi.entity;

import com.sc.scifunapi.enums.*;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("orders")
public class Order {

    @Id
    private String id;

    @Field("user")
    private String userId;  // Node dùng ObjectId → mình dùng String

    @Field("type")
    private OrderType type = OrderType.SUBSCRIPTION; // default như Node

    private double total;

    private Currency currency = Currency.VND;

    private Provider provider = Provider.MOMO;

    @Field("providerRef")
    private String providerRef; // app_trans_id từ ZaloPay

    private OrderStatus status = OrderStatus.PENDING;

    private PlanTier planTier = PlanTier.PRO;

    private Period period = Period.month;

    private Date createdAt = new Date();
    private Date updatedAt = new Date();
}
