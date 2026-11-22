package com.example.carservice.pricing;

import com.example.carservice.exception.InvalidArgumentException;

import java.math.BigDecimal;

/**
 * Immutable result returned by the pricing engine.
 */
public record PricingResult(BigDecimal finalPrice) {
    public PricingResult {
        if (finalPrice == null || finalPrice.signum() < 0) {
            throw new InvalidArgumentException("Final price cannot be null or negative.");
        }
    }
}
