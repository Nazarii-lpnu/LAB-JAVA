package com.example.carservice.pricing;

import com.example.carservice.domain.RepairOrder;

import java.math.BigDecimal;

@FunctionalInterface
public interface PricingPolicy {
    BigDecimal calculatePrice(RepairOrder order);
}
