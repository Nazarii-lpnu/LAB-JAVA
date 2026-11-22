package com.example.carservice.pricing;

import com.example.carservice.domain.Car;
import com.example.carservice.domain.Customer;
import com.example.carservice.domain.enumeration.MechanicSpecialization;
import com.example.carservice.domain.enumeration.OrderPriority;
import com.example.carservice.domain.RepairOrder;
import com.example.carservice.domain.ServiceWork;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CommercialPricingPolicyTest {
    @Test
    void urgentPricingMultipliesLaborOnly() {
        Customer customer = new Customer("Customer");
        RepairOrder order = new RepairOrder(
                new Car("VIN-PRICE", "Brand", "Model", 2020, customer), OrderPriority.URGENT);
        order.addWork(new ServiceWork("Repair", MechanicSpecialization.ENGINE,
                new BigDecimal("100.00"), new BigDecimal("50.00"), Duration.ofHours(1)));

        assertEquals(new BigDecimal("160.00"), policy().calculatePrice(order));
    }

    @Test
    void appliesInjectedRulesToCommercialBasePrice() {
        PricingRuleEngine engine = new PricingRuleEngine(List.of(
                new DiscountPricingRule("discount", new BigDecimal("10")),
                new MinimumPriceRule("minimum", new BigDecimal("150.00"))
        ));
        Customer customer = new Customer("Customer");
        RepairOrder order = new RepairOrder(
                new Car("VIN-RULED", "Brand", "Model", 2020, customer));
        order.addWork(new ServiceWork("Repair", MechanicSpecialization.ENGINE,
                new BigDecimal("50.00"), new BigDecimal("100.00"), Duration.ofHours(1)));

        assertEquals(new BigDecimal("150.00"),
                new CommercialPricingPolicy(engine).calculatePrice(order));
    }

    private static CommercialPricingPolicy policy() {
        return new CommercialPricingPolicy(new PricingRuleEngine(List.of()));
    }
}
