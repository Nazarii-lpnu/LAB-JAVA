package com.example.carservice.pricing;

import com.example.carservice.exception.InvalidArgumentException;

import java.math.BigDecimal;


public record PricingInput(BigDecimal basePrice) {
    public PricingInput {
        if (basePrice == null) {
            throw new InvalidArgumentException("Base price cannot be null.");
        }
        if (basePrice.signum() < 0) {
            throw new InvalidArgumentException("Base price cannot be negative.");
        }
    }
}
