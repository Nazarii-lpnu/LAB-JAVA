package com.example.carservice.service;

import com.example.carservice.PostgresIntegrationTest;
import com.example.carservice.domain.Car;
import com.example.carservice.domain.Customer;
import com.example.carservice.domain.Mechanic;
import com.example.carservice.domain.enumeration.MechanicSpecialization;
import com.example.carservice.domain.RepairOrder;
import com.example.carservice.domain.ServiceWork;
import com.example.carservice.exception.MechanicNotCompatibleException;
import com.example.carservice.repository.RepairOrderRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RepairOrderBatchIntegrationTest extends PostgresIntegrationTest {
    @Autowired
    private RepairOrderService service;

    @Autowired
    private RepairOrderRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void validBatchPersistsAllWorks() {
        RepairOrder order = prepareOrder();
        ServiceWork first = work(MechanicSpecialization.ENGINE);
        ServiceWork second = work(MechanicSpecialization.ENGINE);

        service.addWorks(order.getId(), List.of(first, second));

        RepairOrder persisted = load(order.getId());
        assertEquals(3, persisted.getWorks().size());
        assertEquals(persisted.getWorks().stream()
                        .map(ServiceWork::getId)
                        .sorted()
                        .toList(),
                persisted.getWorks().stream().map(ServiceWork::getId).toList());
    }

    @Test
    void invalidItemRollsBackEarlierBatchItems() {
        RepairOrder order = prepareOrder();
        ServiceWork valid = work(MechanicSpecialization.ENGINE);
        ServiceWork invalid = work(MechanicSpecialization.BRAKES);

        assertThrows(MechanicNotCompatibleException.class,
                () -> service.addWorks(order.getId(), List.of(valid, invalid)));

        RepairOrder persisted = load(order.getId());
        assertEquals(1, persisted.getWorks().size());
        assertEquals(order.getWorks().get(0).getId(), persisted.getWorks().get(0).getId());
    }

    private RepairOrder prepareOrder() {
        Customer customer = new Customer("Batch Customer");
        Car car = new Car("VIN-" + System.nanoTime(), "Brand", "Model", 2020, customer);
        customer.addCar(car);
        Mechanic mechanic = new Mechanic("Batch Mechanic",
                MechanicSpecialization.ENGINE);
        RepairOrder order = new RepairOrder(car);
        ServiceWork existing = work(MechanicSpecialization.ENGINE);
        order.addWork(existing);
        order.diagnose();
        order.approve();
        order.assignMechanic(mechanic);
        new TransactionTemplate(transactionManager).execute(status -> {
            entityManager.persist(customer);
            entityManager.persist(mechanic);
            repository.saveAndFlush(order);
            return null;
        });
        return order;
    }

    private ServiceWork work(MechanicSpecialization specialization) {
        return new ServiceWork("Batch Work", specialization,
                new BigDecimal("10.00"), new BigDecimal("20.00"), Duration.ofHours(1));
    }

    private RepairOrder load(long id) {
        return new TransactionTemplate(transactionManager)
                .execute(status -> repository.findDetailedById(id).orElseThrow());
    }
}
