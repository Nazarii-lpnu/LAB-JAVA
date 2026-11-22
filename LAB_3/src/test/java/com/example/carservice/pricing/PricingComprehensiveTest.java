package com.example.carservice.pricing;

import com.example.carservice.TestFixtures;
import com.example.carservice.domain.enumeration.MechanicSpecialization;
import com.example.carservice.domain.enumeration.OrderPriority;
import com.example.carservice.domain.RepairOrder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PricingComprehensiveTest {
    @ParameterizedTest(name = "parts {0}, labor {1} -> base {2}")
    @MethodSource("basePriceCases")
    void basePriceUsesBigDecimalForPartsAndLabor(String parts, String labor, String expected) {
        RepairOrder order = TestFixtures.order();
        order.addWork(TestFixtures.work(MechanicSpecialization.ENGINE, parts, labor, 1));

        assertEquals(new BigDecimal(expected), order.calculateBaseCost());
    }

    private static Stream<Arguments> basePriceCases() {
        return Stream.of(
                Arguments.of("0.00", "0.00", "0.00"),
                Arguments.of("0.01", "0.00", "0.01"),
                Arguments.of("0.00", "25.50", "25.50"),
                Arguments.of("12.34", "56.78", "69.12"),
                Arguments.of("1.005", "1.005", "2.01")
        );
    }

    @Test
    void zeroNormalOrderHasZeroCommercialPrice() {
        assertEquals(new BigDecimal("0.00"),
                policy().calculatePrice(TestFixtures.order()));
    }

    @Test
    void normalOrderWithOneLaborItemUsesLaborAmount() {
        RepairOrder order = TestFixtures.order();
        order.addWork(TestFixtures.work(MechanicSpecialization.ENGINE, "0.00", "45.00", 1));

        assertEquals(new BigDecimal("45.00"), policy().calculatePrice(order));
    }

    @Test
    void normalOrderWithMultipleLaborItemsSumsLabor() {
        RepairOrder order = TestFixtures.order();
        order.addWork(TestFixtures.work(MechanicSpecialization.ENGINE, "0.00", "10.00", 1));
        order.addWork(TestFixtures.work(MechanicSpecialization.BRAKES, "0.00", "15.50", 1));

        assertEquals(new BigDecimal("25.50"), policy().calculatePrice(order));
    }

    @Test
    void normalOrderIncludesSpareParts() {
        RepairOrder order = TestFixtures.order();
        order.addWork(TestFixtures.work(MechanicSpecialization.ENGINE, "125.75", "0.00", 1));

        assertEquals(new BigDecimal("125.75"), policy().calculatePrice(order));
    }

    @Test
    void urgentCommercialPriceMultipliesLaborButNotParts() {
        RepairOrder order = new RepairOrder(TestFixtures.car(TestFixtures.customer()),
                OrderPriority.URGENT);
        order.addWork(TestFixtures.work(MechanicSpecialization.ENGINE, "100.00", "50.00", 1));

        assertEquals(new BigDecimal("160.00"), policy().calculatePrice(order));
    }

    @ParameterizedTest(name = "discount {0}% on {1} -> {2}")
    @MethodSource("discountCases")
    void discountRuleHandlesZeroFullAndFractionalDiscounts(String percentage, String amount, String expected) {
        PricingRuleEngine engine = new PricingRuleEngine(List.of(
                new DiscountPricingRule("discount", new BigDecimal(percentage))));

        assertEquals(new BigDecimal(expected),
                engine.calculate(new PricingInput(new BigDecimal(amount))).finalPrice());
    }

    private static Stream<Arguments> discountCases() {
        return Stream.of(
                Arguments.of("0", "100.00", "100.00"),
                Arguments.of("100", "100.00", "0.00"),
                Arguments.of("12.5", "80.00", "70.00"),
                Arguments.of("10", "0.01", "0.01")
        );
    }

    @Test
    void minimumRuleRaisesAmountBelowFloor() {
        PricingRuleEngine engine = new PricingRuleEngine(List.of(
                new MinimumPriceRule("minimum", new BigDecimal("50.00"))));

        assertEquals(new BigDecimal("50.00"),
                engine.calculate(new PricingInput(new BigDecimal("49.99"))).finalPrice());
    }

    @Test
    void minimumRuleLeavesAmountAboveFloorUnchanged() {
        PricingRuleEngine engine = new PricingRuleEngine(List.of(
                new MinimumPriceRule("minimum", new BigDecimal("50.00"))));

        assertEquals(new BigDecimal("50.01"),
                engine.calculate(new PricingInput(new BigDecimal("50.01"))).finalPrice());
    }

    @ParameterizedTest(name = "amount {0} and floor {1} -> {2}")
    @MethodSource("minimumBoundaryCases")
    void minimumRuleHandlesExactBoundary(String amount, String floor, String expected) {
        PricingRuleEngine engine = new PricingRuleEngine(List.of(
                new MinimumPriceRule("minimum", new BigDecimal(floor))));

        assertEquals(new BigDecimal(expected),
                engine.calculate(new PricingInput(new BigDecimal(amount))).finalPrice());
    }

    private static Stream<Arguments> minimumBoundaryCases() {
        return Stream.of(
                Arguments.of("0.00", "0.00", "0.00"),
                Arguments.of("50.00", "50.00", "50.00"),
                Arguments.of("50.001", "50.00", "50.00")
        );
    }

    @Test
    void commercialPolicyRejectsNullOrder() {
        assertThrows(com.example.carservice.exception.InvalidArgumentException.class,
                () -> policy().calculatePrice(null));
    }

    @Test
    void injectedRuleEngineControlsCommercialResult() {
        PricingRuleEngine engine = new PricingRuleEngine(List.of(
                new DiscountPricingRule("discount", new BigDecimal("10"))));
        RepairOrder order = TestFixtures.order();
        order.addWork(TestFixtures.work(MechanicSpecialization.ENGINE, "40.00", "60.00", 1));

        assertEquals(new BigDecimal("90.00"),
                new CommercialPricingPolicy(engine).calculatePrice(order));
    }

    private static CommercialPricingPolicy policy() {
        return new CommercialPricingPolicy(new PricingRuleEngine(List.of()));
    }
}
