package com.example.carservice.pricing;

import com.example.carservice.exception.InvalidArgumentException;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Immutable result returned by the pricing engine.
 */
public record PricingResult(BigDecimal finalPrice) {
    public PricingResult {
        if (Objects.isNull(finalPrice) || finalPrice.signum() < 0) {
            throw new InvalidArgumentException("Final price cannot be null or negative.");
        }
    }
}
