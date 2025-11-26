package com.sc.scifunapi.entity;

import com.sc.scifunapi.enums.*;
import lombok.*;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.NONE; // NONE | ACTIVE | CANCELED

    // chỉ set khi ACTIVE
    private SubscriptionTier tier; // PRO

    private Date currentPeriodEnd;

    private PaymentProvider provider; // ZALOPAY
}
