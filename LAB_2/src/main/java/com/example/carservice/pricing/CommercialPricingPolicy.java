package com.example.carservice.pricing;

import com.example.carservice.domain.OrderPriority;
import com.example.carservice.domain.RepairOrder;
import com.example.carservice.exception.InvalidArgumentException;

import java.math.BigDecimal;

public class CommercialPricingPolicy implements PricingPolicy {
    private static BigDecimal URGENT_LABOR_MULTIPLIER = new BigDecimal("1.20");
    private PricingRuleEngine ruleEngine;

    public CommercialPricingPolicy() {
        this(new PricingRuleEngine(java.util.List.of()));
    }

    public CommercialPricingPolicy(PricingRuleEngine ruleEngine) {
        if (ruleEngine == null) throw new InvalidArgumentException("Pricing rule engine cannot be null.");
        this.ruleEngine = ruleEngine;
    }

    @Override
    public BigDecimal calculatePrice(RepairOrder order) {
        if (order == null) throw new InvalidArgumentException("Repair order cannot be null.");
        BigDecimal base = order.calculateBaseCost();
        if (order.getPriority() == OrderPriority.NORMAL) {
            return ruleEngine.calculate(new PricingInput(base)).finalPrice();
        }
        BigDecimal parts = order.getWorks().stream()
                .map(work -> work.getPartsCost())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal labor = order.getWorks().stream()
                .map(work -> work.getLaborCost())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal commercialBase = parts.add(labor.multiply(URGENT_LABOR_MULTIPLIER));
        return ruleEngine.calculate(new PricingInput(commercialBase)).finalPrice();
    }
}
