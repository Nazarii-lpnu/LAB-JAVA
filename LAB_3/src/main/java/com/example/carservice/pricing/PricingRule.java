package com.example.carservice.pricing;

/**
 * A deterministic, single-purpose transformation in a pricing pipeline.
 * Lower priorities execute first. Rules must return a new context.
 */
public interface PricingRule {
    String id();

    int priority();

    PricingContext apply(PricingContext context);
}
