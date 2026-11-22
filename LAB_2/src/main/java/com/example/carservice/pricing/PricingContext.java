package com.example.carservice.pricing;

import com.example.carservice.exception.InvalidArgumentException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;


public class PricingContext {
    private BigDecimal originalPrice;
    private BigDecimal currentPrice;
    private Set<String> appliedRuleIds;

    private PricingContext(BigDecimal originalPrice, BigDecimal currentPrice, Set<String> appliedRuleIds) {
        this.originalPrice = originalPrice;
        this.currentPrice = currentPrice;
        this.appliedRuleIds = Collections.unmodifiableSet(appliedRuleIds);
    }

    public static PricingContext start(BigDecimal basePrice) {
        if (basePrice == null || basePrice.signum() < 0) {
            throw new InvalidArgumentException("Base price cannot be null or negative.");
        }
        return new PricingContext(basePrice, basePrice, new LinkedHashSet<>());
    }

    public BigDecimal originalPrice() {
        return originalPrice;
    }

    public BigDecimal currentPrice() {
        return currentPrice;
    }

    public Set<String> appliedRuleIds() {
        return appliedRuleIds;
    }

    public PricingContext withPrice(String ruleId, BigDecimal newPrice) {
        if (ruleId == null || ruleId.isBlank()) {
            throw new InvalidArgumentException("Pricing rule id cannot be null, empty, or blank.");
        }
        if (newPrice == null || newPrice.signum() < 0) {
            throw new InvalidArgumentException("Rule result price cannot be null or negative.");
        }
        Set<String> updatedRuleIds = new LinkedHashSet<>(appliedRuleIds);
        updatedRuleIds.add(ruleId);
        return new PricingContext(originalPrice, newPrice, updatedRuleIds);
    }
}
