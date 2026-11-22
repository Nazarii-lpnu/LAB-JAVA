package com.example.carservice.pricing;

import com.example.carservice.exception.InvalidArgumentException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;


public class DiscountPricingRule implements PricingRule {
    private static final int PRIORITY = 100;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private final String id;
    private final BigDecimal percentage;

    public DiscountPricingRule(String id, BigDecimal percentage) {
        if (Objects.isNull(id) || id.isBlank()) {
            throw new InvalidArgumentException("Discount rule id cannot be null, empty, or blank.");
        }
        if (Objects.isNull(percentage) || percentage.signum() < 0 || percentage.compareTo(ONE_HUNDRED) > 0) {
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
        if (Objects.isNull(context)) {
            throw new InvalidArgumentException("Pricing context cannot be null.");
        }
    }
}
