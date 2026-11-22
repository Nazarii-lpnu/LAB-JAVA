package com.example.carservice.pricing;

import com.example.carservice.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class PricingRuleEngineTest {
    @Test
    void appliesDiscountBeforeMinimumPrice() {
        PricingRuleEngine engine = new PricingRuleEngine(List.of(
                new MinimumPriceRule("minimum", new BigDecimal("90.00")),
                new DiscountPricingRule("discount", new BigDecimal("20"))
        ));

        assertEquals(new BigDecimal("90.00"),
                engine.calculate(new PricingInput(new BigDecimal("100.00"))).finalPrice());
    }

    @Test
    void appliesBoundaryDiscountAndMinimumValues() {
        PricingRuleEngine engine = new PricingRuleEngine(List.of(
                new DiscountPricingRule("full-discount", new BigDecimal("100")),
                new MinimumPriceRule("floor", new BigDecimal("10.00"))
        ));

        assertEquals(new BigDecimal("10.00"),
                engine.calculate(new PricingInput(new BigDecimal("100.00"))).finalPrice());
    }

    @Test
    void rejectsDuplicateRuleIds() {
        assertThrows(InvalidArgumentException.class, () -> new PricingRuleEngine(List.of(
                new DiscountPricingRule("same", new BigDecimal("10")),
                new MinimumPriceRule("same", new BigDecimal("1"))
        )));
    }

    @Test
    void rejectsInvalidInputsAndDiscountLimits() {
        assertThrows(InvalidArgumentException.class,
                () -> new PricingInput(new BigDecimal("-1.00")));
        assertThrows(InvalidArgumentException.class,
                () -> new DiscountPricingRule("invalid", new BigDecimal("100.01")));
        assertThrows(InvalidArgumentException.class,
                () -> new MinimumPriceRule("invalid", new BigDecimal("-0.01")));
    }

    @Test
    void repeatedInvocationIsDeterministicAndDoesNotMutateInput() {
        PricingInput input = new PricingInput(new BigDecimal("123.456"));
        PricingRuleEngine engine = new PricingRuleEngine(List.of(
                new DiscountPricingRule("discount", new BigDecimal("10"))
        ));

        PricingResult first = engine.calculate(input);
        PricingResult second = engine.calculate(input);

        assertEquals(new BigDecimal("123.456"), input.basePrice());
        assertEquals(first, second);
        assertEquals(new BigDecimal("111.11"), first.finalPrice());
    }

    @Test
    void acceptsNewRuleWithoutChangingEngineAlgorithm() {
        PricingRule surcharge = new PricingRule() {
            @Override
            public String id() {
                return "fixed-surcharge";
            }

            @Override
            public int priority() {
                return 50;
            }

            @Override
            public PricingContext apply(PricingContext context) {
                return context.withPrice(id(), context.currentPrice().add(new BigDecimal("5.00")));
            }
        };

        PricingRuleEngine engine = new PricingRuleEngine(List.of(surcharge));

        assertEquals(new BigDecimal("105.00"),
                engine.calculate(new PricingInput(new BigDecimal("100.00"))).finalPrice());
    }

    @Test
    void emptyRuleCollectionReturnsRoundedInput() {
        PricingRuleEngine engine = new PricingRuleEngine(List.of());

        assertEquals(new BigDecimal("12.35"),
                engine.calculate(new PricingInput(new BigDecimal("12.345"))).finalPrice());
    }

    @Test
    void rejectsNullInput() {
        assertThrows(InvalidArgumentException.class,
                () -> new PricingRuleEngine(List.of()).calculate(null));
    }

    @Test
    void rejectsNullRuleCollectionAndNullRule() {
        assertThrows(InvalidArgumentException.class, () -> new PricingRuleEngine(null));
        assertThrows(InvalidArgumentException.class,
                () -> new PricingRuleEngine(Arrays.asList((PricingRule) null)));
    }

    @Test
    void rejectsRuleReturningNullContext() {
        PricingRule broken = new PricingRule() {
            public String id() { return "broken"; }
            public int priority() { return 1; }
            public PricingContext apply(PricingContext context) { return null; }
        };

        assertThrows(InvalidArgumentException.class,
                () -> new PricingRuleEngine(List.of(broken))
                        .calculate(new PricingInput(new BigDecimal("10.00"))));
    }

    @Test
    void rejectsRuleThatDoesNotRecordItsApplication() {
        PricingRule broken = new PricingRule() {
            public String id() { return "unrecorded"; }
            public int priority() { return 1; }
            public PricingContext apply(PricingContext context) {
                return PricingContext.start(context.currentPrice());
            }
        };

        assertThrows(InvalidArgumentException.class,
                () -> new PricingRuleEngine(List.of(broken))
                        .calculate(new PricingInput(new BigDecimal("10.00"))));
    }

    @Test
    void equalPriorityRulesAreOrderedById() {
        PricingRule addA = addingRule("a", "1.00", 10);
        PricingRule addB = addingRule("b", "2.00", 10);

        PricingRuleEngine first = new PricingRuleEngine(List.of(addB, addA));
        PricingRuleEngine second = new PricingRuleEngine(List.of(addA, addB));

        assertEquals(first.calculate(new PricingInput(new BigDecimal("10.00"))),
                second.calculate(new PricingInput(new BigDecimal("10.00"))));
        assertEquals(new BigDecimal("13.00"),
                first.calculate(new PricingInput(new BigDecimal("10.00"))).finalPrice());
    }

    @Test
    void ruleFailureIsPropagatedWithoutSilentFallback() {
        PricingRule failing = new PricingRule() {
            public String id() { return "failing"; }
            public int priority() { return 1; }
            public PricingContext apply(PricingContext context) {
                throw new IllegalStateException("rule failure");
            }
        };

        assertThrows(IllegalStateException.class,
                () -> new PricingRuleEngine(List.of(failing))
                        .calculate(new PricingInput(new BigDecimal("10.00"))));
    }

    @Test
    void contextTransformationDoesNotMutatePreviousContext() {
        PricingContext original = PricingContext.start(new BigDecimal("100.00"));
        PricingContext changed = original.withPrice("discount", new BigDecimal("90.00"));

        assertEquals(new BigDecimal("100.00"), original.currentPrice());
        assertEquals(new BigDecimal("90.00"), changed.currentPrice());
        assertNotSame(original, changed);
    }

    @Test
    void appliedRuleIdsAreImmutableToCallers() {
        PricingContext context = PricingContext.start(new BigDecimal("10.00"))
                .withPrice("rule", new BigDecimal("9.00"));

        assertThrows(UnsupportedOperationException.class,
                () -> context.appliedRuleIds().add("external"));
    }

    @Test
    void rulesWithDifferentPrioritiesHaveDifferentDocumentedOrder() {
        PricingRule floorFirst = new MinimumPriceRule("floor", new BigDecimal("90.00"));
        PricingRule discount = new DiscountPricingRule("discount", new BigDecimal("20"));
        PricingRuleEngine engine = new PricingRuleEngine(List.of(discount, floorFirst));

        assertEquals(new BigDecimal("90.00"),
                engine.calculate(new PricingInput(new BigDecimal("100.00"))).finalPrice());
    }

    private static PricingRule addingRule(String id, String amount, int priority) {
        return new PricingRule() {
            public String id() { return id; }
            public int priority() { return priority; }
            public PricingContext apply(PricingContext context) {
                return context.withPrice(id, context.currentPrice().add(new BigDecimal(amount)));
            }
        };
    }
}
