package com.example.carservice.pricing;

import com.example.carservice.domain.ServiceWork;
import com.example.carservice.domain.enumeration.OrderPriority;
import com.example.carservice.domain.RepairOrder;
import com.example.carservice.exception.InvalidArgumentException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;

@Component
public class CommercialPricingPolicy implements PricingPolicy {
    private static final BigDecimal URGENT_LABOR_MULTIPLIER = new BigDecimal("1.20");
    private final PricingRuleEngine ruleEngine;

    public CommercialPricingPolicy(PricingRuleEngine ruleEngine) {
        if (Objects.isNull(ruleEngine)) throw new InvalidArgumentException("Pricing rule engine cannot be null.");
        this.ruleEngine = ruleEngine;
    }

    @Override
    public BigDecimal calculatePrice(RepairOrder order) {
        if (Objects.isNull(order)) throw new InvalidArgumentException("Repair order cannot be null.");
        BigDecimal base = order.calculateBaseCost();
        if (Objects.equals(order.getPriority(), OrderPriority.NORMAL)) {
            return ruleEngine.calculate(new PricingInput(base)).finalPrice();
        }
        BigDecimal parts = order.getWorks().stream()
                .map(ServiceWork::getPartsCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal labor = order.getWorks().stream()
                .map(ServiceWork::getLaborCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal commercialBase = parts.add(labor.multiply(URGENT_LABOR_MULTIPLIER));
        return ruleEngine.calculate(new PricingInput(commercialBase)).finalPrice();
    }
}
