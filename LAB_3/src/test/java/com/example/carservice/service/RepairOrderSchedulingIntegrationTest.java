package com.example.carservice.service;

import com.example.carservice.PostgresIntegrationTest;
import com.example.carservice.domain.Car;
import com.example.carservice.domain.Customer;
import com.example.carservice.domain.Mechanic;
import com.example.carservice.domain.enumeration.MechanicSpecialization;
import com.example.carservice.domain.RepairOrder;
import com.example.carservice.domain.ServiceWork;
import com.example.carservice.domain.TimeSlot;
import com.example.carservice.domain.enumeration.OrderStatus;
import com.example.carservice.repository.RepairOrderRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RepairOrderSchedulingIntegrationTest extends PostgresIntegrationTest {
    @Autowired
    private RepairOrderService service;

    @Autowired
    private RepairOrderRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void startsPlannedOrderWithoutConflictingWithItself() {
        RepairOrder order = createPlannedOrder();

        service.start(order.getId());

        RepairOrder persisted = execute(() -> repository.findById(order.getId()).orElseThrow());
        assertEquals(OrderStatus.IN_PROGRESS, persisted.getStatus());
    }

    private RepairOrder createPlannedOrder() {
        Customer customer = new Customer("Scheduling Customer");
        Car car = new Car("VIN-SCHEDULE-" + System.nanoTime(), "Brand", "Model", 2020, customer);
        customer.addCar(car);
        Mechanic mechanic = new Mechanic("Scheduling Mechanic",
                MechanicSpecialization.GENERAL);
        ServiceWork work = new ServiceWork("Scheduling Work",
                MechanicSpecialization.ENGINE, new BigDecimal("10.00"),
                new BigDecimal("20.00"), Duration.ofHours(1));
        RepairOrder order = new RepairOrder(car);
        order.addWork(work);
        order.diagnose();
        order.approve();
        order.assignMechanic(mechanic);
        order.plan(new TimeSlot(Instant.parse("2026-01-01T09:00:00Z"), Duration.ofHours(2)));

        execute(() -> {
            entityManager.persist(customer);
            entityManager.persist(mechanic);
            repository.saveAndFlush(order);
            return null;
        });
        return order;
    }

    private <T> T execute(Supplier<T> action) {
        return new TransactionTemplate(transactionManager).execute(status -> action.get());
    }
}
