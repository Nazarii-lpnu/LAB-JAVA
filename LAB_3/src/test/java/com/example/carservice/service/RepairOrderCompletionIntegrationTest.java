package com.example.carservice.service;

import com.example.carservice.PostgresIntegrationTest;
import com.example.carservice.domain.Car;
import com.example.carservice.domain.Customer;
import com.example.carservice.domain.Mechanic;
import com.example.carservice.domain.enumeration.MechanicSpecialization;
import com.example.carservice.domain.enumeration.OrderStatus;
import com.example.carservice.domain.RepairOrder;
import com.example.carservice.domain.ServiceWork;
import com.example.carservice.exception.EntityNotFoundException;
import com.example.carservice.exception.InvalidStateTransitionException;
import com.example.carservice.pricing.PricingPolicy;
import com.example.carservice.repository.RepairOrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

class RepairOrderCompletionIntegrationTest extends PostgresIntegrationTest {
    @Autowired
    private RepairOrderService service;

    @Autowired
    private RepairOrderRepository repository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private PricingPolicy pricingPolicy;

    @Test
    void successfulCompletionPersistsPriceStatusAndTimestamp() {
        RepairOrder order = prepareOrder(true);
        BigDecimal price = new BigDecimal("123.45");
        when(pricingPolicy.calculatePrice(org.mockito.ArgumentMatchers.any())).thenReturn(price);

        service.complete(order.getId());

        RepairOrder persisted = load(order.getId());
        assertEquals(new BigDecimal("123.45"), persisted.getFinalCommercialPrice());
        assertEquals(OrderStatus.COMPLETED, persisted.getStatus());
        assertNotNull(persisted.getCompletedAt());
    }

    @Test
    void unfinishedWorksPreventCompletionAndPersistNoChanges() {
        RepairOrder order = prepareOrder(false);
        when(pricingPolicy.calculatePrice(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new BigDecimal("123.45"));

        assertThrows(InvalidStateTransitionException.class, () -> service.complete(order.getId()));

        RepairOrder persisted = load(order.getId());
        assertEquals(OrderStatus.IN_PROGRESS, persisted.getStatus());
        assertNull(persisted.getFinalCommercialPrice());
        assertNull(persisted.getCompletedAt());
    }

    @Test
    void invalidStatusPreventsCompletionAndDoesNotPriceOrder() {
        RepairOrder order = createOrder(false, OrderStatus.CREATED);

        assertThrows(InvalidStateTransitionException.class, () -> service.complete(order.getId()));

        org.mockito.Mockito.verifyNoInteractions(pricingPolicy);
        assertEquals(OrderStatus.CREATED, load(order.getId()).getStatus());
    }

    @Test
    void missingOrderIsReported() {
        long id = Long.MAX_VALUE;

        assertThrows(EntityNotFoundException.class, () -> service.complete(id));
    }

    @Test
    void pricingFailureLeavesDatabaseUnchanged() {
        RepairOrder order = prepareOrder(true);
        doThrow(new IllegalStateException("pricing failed"))
                .when(pricingPolicy).calculatePrice(org.mockito.ArgumentMatchers.any());

        assertThrows(IllegalStateException.class, () -> service.complete(order.getId()));

        RepairOrder persisted = load(order.getId());
        assertEquals(OrderStatus.IN_PROGRESS, persisted.getStatus());
        assertNull(persisted.getFinalCommercialPrice());
        assertNull(persisted.getCompletedAt());
    }

    @Test
    void persistenceFailureRollsBackStatusPriceAndTimestamp() {
        RepairOrder order = prepareOrder(true);
        when(pricingPolicy.calculatePrice(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new BigDecimal("999999999999999999999.00"));

        assertThrows(DataAccessException.class, () -> service.complete(order.getId()));

        RepairOrder persisted = load(order.getId());
        assertEquals(OrderStatus.IN_PROGRESS, persisted.getStatus());
        assertNull(persisted.getFinalCommercialPrice());
        assertNull(persisted.getCompletedAt());
    }

    private RepairOrder prepareOrder(boolean completedWork) {
        return createOrder(completedWork, OrderStatus.IN_PROGRESS);
    }

    private RepairOrder createOrder(boolean completedWork, OrderStatus status) {
        Customer customer = new Customer("Integration Customer");
        Car car = new Car("VIN-" + System.nanoTime(), "Brand", "Model", 2020, customer);
        customer.addCar(car);
        Mechanic mechanic = new Mechanic("Integration Mechanic",
                MechanicSpecialization.GENERAL);
        ServiceWork work = new ServiceWork("Integration Work",
                MechanicSpecialization.ENGINE, new BigDecimal("10.00"),
                new BigDecimal("20.00"), Duration.ofHours(1));
        RepairOrder order = new RepairOrder(car);
        order.addWork(work);
        if (!Objects.equals(status, OrderStatus.CREATED)) {
            order.diagnose();
            order.approve();
            order.assignMechanic(mechanic);
            if (Objects.equals(status, OrderStatus.IN_PROGRESS)) {
                order.start();
            }
        }
        RepairOrder persisted = execute(() -> {
            entityManager.persist(customer);
            entityManager.persist(mechanic);
            return repository.saveAndFlush(order);
        });
        if (completedWork) {
            execute(() -> {
                RepairOrder current = repository.findById(persisted.getId()).orElseThrow();
                current.completeWork(current.getWorks().get(0).getId());
                repository.saveAndFlush(current);
                return null;
            });
        }
        return persisted;
    }

    private RepairOrder load(long id) {
        return execute(() -> repository.findById(id).orElseThrow());
    }

    private <T> T execute(Supplier<T> action) {
        return new TransactionTemplate(transactionManager).execute(status -> action.get());
    }

    @Autowired
    private jakarta.persistence.EntityManager entityManager;
}
