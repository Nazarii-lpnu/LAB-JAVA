package com.example.carservice.pricing;

import com.example.carservice.exception.InvalidArgumentException;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class PricingRuleEngine {
    public static int MONEY_SCALE = 2;
    public static RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private List<PricingRule> rules;

    public PricingRuleEngine(Collection<? extends PricingRule> rules) {
        if (rules == null) {
            throw new InvalidArgumentException("Pricing rules cannot be null.");
        }
        List<PricingRule> configuredRules = new ArrayList<>(rules);
        Set<String> ids = new HashSet<>();
        for (PricingRule rule : configuredRules) {
            if (rule == null) {
                throw new InvalidArgumentException("Pricing rules cannot contain null.");
            }
            if (rule.id() == null || rule.id().isBlank()) {
                throw new InvalidArgumentException("Pricing rule id cannot be null, empty, or blank.");
            }
            if (!ids.add(rule.id())) {
                throw new InvalidArgumentException("Duplicate pricing rule id '" + rule.id() + "'.");
            }
        }
        configuredRules.sort(Comparator.comparingInt(PricingRule::priority).thenComparing(PricingRule::id));
        this.rules = List.copyOf(configuredRules);
    }

    public PricingResult calculate(PricingInput input) {
        if (input == null) {
            throw new InvalidArgumentException("Pricing input cannot be null.");
        }
        PricingContext context = PricingContext.start(input.basePrice());
        for (PricingRule rule : rules) {
            if (context.appliedRuleIds().contains(rule.id())) {
                throw new InvalidArgumentException("Pricing rule '" + rule.id() + "' would be applied more than once.");
            }
            PricingContext next = rule.apply(context);
            if (next == null) {
                throw new InvalidArgumentException("Pricing rule '" + rule.id() + "' returned a null context.");
            }
            if (next.currentPrice().signum() < 0) {
                throw new InvalidArgumentException("Pricing rule '" + rule.id() + "' produced a negative price.");
            }
            if (!next.appliedRuleIds().contains(rule.id())) {
                throw new InvalidArgumentException("Pricing rule '" + rule.id()
                        + "' did not record its application in the returned context.");
            }
            context = next;
        }
        return new PricingResult(context.currentPrice().setScale(MONEY_SCALE, ROUNDING_MODE));
    }
}
