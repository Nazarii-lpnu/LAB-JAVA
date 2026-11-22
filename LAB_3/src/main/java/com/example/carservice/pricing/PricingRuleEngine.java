package com.example.carservice.pricing;

import com.example.carservice.exception.InvalidArgumentException;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;


public class PricingRuleEngine {
    private static final int MONEY_SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private final List<PricingRule> rules;

    public PricingRuleEngine(Collection<? extends PricingRule> rules) {
        if (Objects.isNull(rules)) {
            throw new InvalidArgumentException("Pricing rules cannot be null.");
        }
        List<PricingRule> configuredRules = new ArrayList<>(rules);
        Set<String> ids = new HashSet<>();
        configuredRules.stream().forEach(rule -> {
            if (Objects.isNull(rule)) {
                throw new InvalidArgumentException("Pricing rules cannot contain null.");
            }
            if (Objects.isNull(rule.id()) || rule.id().isBlank()) {
                throw new InvalidArgumentException("Pricing rule id cannot be null, empty, or blank.");
            }
            if (!ids.add(rule.id())) {
                throw new InvalidArgumentException("Duplicate pricing rule id '" + rule.id() + "'.");
            }
        });
        configuredRules.sort(Comparator.comparingInt(PricingRule::priority).thenComparing(PricingRule::id));
        this.rules = List.copyOf(configuredRules);
    }

    public PricingResult calculate(PricingInput input) {
        if (Objects.isNull(input)) {
            throw new InvalidArgumentException("Pricing input cannot be null.");
        }
        // Rule application is sequential because each rule consumes the previous rule's context.
        PricingContext context = rules.stream().reduce(
                PricingContext.start(input.basePrice()),
                PricingRuleEngine::applyRule,
                (first, second) -> {
                    throw new IllegalStateException("Pricing rules must be reduced sequentially.");
                });
        return new PricingResult(context.currentPrice().setScale(MONEY_SCALE, ROUNDING_MODE));
    }

    private static PricingContext applyRule(PricingContext context, PricingRule rule) {
        if (context.appliedRuleIds().contains(rule.id())) {
            throw new InvalidArgumentException("Pricing rule '" + rule.id() + "' would be applied more than once.");
        }
        PricingContext next = rule.apply(context);
        if (Objects.isNull(next)) {
            throw new InvalidArgumentException("Pricing rule '" + rule.id() + "' returned a null context.");
        }
        if (next.currentPrice().signum() < 0) {
            throw new InvalidArgumentException("Pricing rule '" + rule.id() + "' produced a negative price.");
        }
        if (!next.appliedRuleIds().contains(rule.id())) {
            throw new InvalidArgumentException("Pricing rule '" + rule.id()
                    + "' did not record its application in the returned context.");
        }
        return next;
    }
}
