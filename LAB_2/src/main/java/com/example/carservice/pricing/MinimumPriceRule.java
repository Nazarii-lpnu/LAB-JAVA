package com.example.carservice.pricing;

import com.example.carservice.exception.InvalidArgumentException;

import java.math.BigDecimal;


public class MinimumPriceRule implements PricingRule {
    private static int PRIORITY = 1_000;

    private String id;
    private BigDecimal minimumPrice;

    public MinimumPriceRule(String id, BigDecimal minimumPrice) {
        if (id == null || id.isBlank()) {
            throw new InvalidArgumentException("Minimum-price rule id cannot be null, empty, or blank.");
        }
        if (minimumPrice == null || minimumPrice.signum() < 0) {
            throw new InvalidArgumentException("Minimum price cannot be null or negative.");
        }
        this.id = id;
        this.minimumPrice = minimumPrice;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public int priority() {
        return PRIORITY;
    }

    @Override
    public PricingContext apply(PricingContext context) {
        if (context == null) {
            throw new InvalidArgumentException("Pricing context cannot be null.");
        }
        return context.withPrice(id, context.currentPrice().max(minimumPrice));
    }
}
