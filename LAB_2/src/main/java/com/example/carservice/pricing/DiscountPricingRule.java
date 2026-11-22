package com.example.carservice.pricing;

import com.example.carservice.exception.InvalidArgumentException;

import java.math.BigDecimal;
import java.math.RoundingMode;


public class DiscountPricingRule implements PricingRule {
    private static int PRIORITY = 100;
    private static BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private String id;
    private BigDecimal percentage;

    public DiscountPricingRule(String id, BigDecimal percentage) {
        if (id == null || id.isBlank()) {
            throw new InvalidArgumentException("Discount rule id cannot be null, empty, or blank.");
        }
        if (percentage == null || percentage.signum() < 0 || percentage.compareTo(ONE_HUNDRED) > 0) {
            throw new InvalidArgumentException("Discount percentage must be between 0 and 100.");
        }
        this.id = id;
        this.percentage = percentage;
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
        requireContext(context);
        BigDecimal multiplier = ONE_HUNDRED.subtract(percentage)
                .divide(ONE_HUNDRED, 10, RoundingMode.HALF_UP);
        BigDecimal discounted = context.currentPrice().multiply(multiplier);
        return context.withPrice(id, discounted);
    }

    private void requireContext(PricingContext context) {
        if (context == null) {
            throw new InvalidArgumentException("Pricing context cannot be null.");
        }
    }
}
