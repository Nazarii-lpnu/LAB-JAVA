package com.example.carservice.pricing;

import com.example.carservice.PostgresIntegrationTest;
import com.example.carservice.domain.Car;
import com.example.carservice.domain.Customer;
import com.example.carservice.domain.Mechanic;
import com.example.carservice.domain.enumeration.MechanicSpecialization;
import com.example.carservice.domain.RepairOrder;
import com.example.carservice.domain.ServiceWork;
import com.example.carservice.repository.RepairOrderRepository;
import com.example.carservice.service.RepairOrderService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PricingConfigurationIntegrationTest extends PostgresIntegrationTest {
    @Autowired
    private RepairOrderService service;

    @Autowired
    private RepairOrderRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void completedOrderUsesConfiguredDiscountAndMinimumPrice() {
        RepairOrder order = createOrder();

        service.complete(order.getId());

        RepairOrder persisted = execute(() -> repository.findById(order.getId()).orElseThrow());
        assertEquals(new BigDecimal("50.00"), persisted.getFinalCommercialPrice());
    }

    private RepairOrder createOrder() {
        Customer customer = new Customer("Pricing Customer");
        Car car = new Car("VIN-PRICING-" + System.nanoTime(), "Brand", "Model", 2020, customer);
        customer.addCar(car);
        Mechanic mechanic = new Mechanic("Pricing Mechanic",
                MechanicSpecialization.GENERAL);
        ServiceWork work = new ServiceWork("Pricing Work",
                MechanicSpecialization.ENGINE, new BigDecimal("10.00"),
                new BigDecimal("45.00"), Duration.ofHours(1));
        RepairOrder order = new RepairOrder(car);
        order.addWork(work);
        order.diagnose();
        order.approve();
        order.assignMechanic(mechanic);
        order.start();

        RepairOrder persisted = execute(() -> {
            entityManager.persist(customer);
            entityManager.persist(mechanic);
            return repository.saveAndFlush(order);
        });
        execute(() -> {
            RepairOrder current = repository.findById(persisted.getId()).orElseThrow();
            current.completeWork(current.getWorks().get(0).getId());
            repository.saveAndFlush(current);
            return null;
        });
        return persisted;
    }

    private <T> T execute(Supplier<T> action) {
        return new TransactionTemplate(transactionManager).execute(status -> action.get());
    }
}
