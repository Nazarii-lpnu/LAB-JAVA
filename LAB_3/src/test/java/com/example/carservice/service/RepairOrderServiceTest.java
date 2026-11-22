package com.example.carservice.service;

import com.example.carservice.domain.Car;
import com.example.carservice.domain.Customer;
import com.example.carservice.domain.RepairOrder;
import com.example.carservice.domain.ServiceWork;
import com.example.carservice.domain.enumeration.MechanicSpecialization;
import com.example.carservice.exception.ConcurrentModificationException;
import com.example.carservice.pricing.PricingPolicy;
import com.example.carservice.repository.RepairOrderRepository;
import com.example.carservice.service.impl.RepairOrderServiceImpl;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class RepairOrderServiceTest {
    @Test
    void calculatesCommercialPriceThroughInjectedPolicyAndPersistsOrder() {
        RepairOrderRepository repository = mock(RepairOrderRepository.class);
        PricingPolicy policy = mock(PricingPolicy.class);
        Customer customer = new Customer("Customer");
        RepairOrder order = new RepairOrder(
                new Car("VIN-SERVICE", "Brand", "Model", 2020, customer));
        order.addWork(new ServiceWork("Work", MechanicSpecialization.ENGINE,
                new BigDecimal("10.00"), new BigDecimal("20.00"), Duration.ofHours(1)));
        long orderId = 1L;
        when(repository.findById(orderId)).thenReturn(Optional.of(order));
        when(policy.calculatePrice(order)).thenReturn(new BigDecimal("42.00"));

        RepairOrderService service = new RepairOrderServiceImpl(repository, policy);

        assertEquals(new BigDecimal("42.00"), service.calculateCommercialPrice(orderId));
        verify(policy).calculatePrice(order);
        verify(repository).saveAndFlush(order);
    }

    @Test
    void translatesOptimisticLockingConflictIntoDomainException() {
        RepairOrderRepository repository = mock(RepairOrderRepository.class);
        RepairOrder order = new RepairOrder(
                new Car("VIN-CONFLICT", "Brand", "Model", 2020,
                        new Customer("Customer")));
        doThrow(new ObjectOptimisticLockingFailureException(
                RepairOrder.class, 1L)).when(repository).saveAndFlush(order);

        RepairOrderService service = new RepairOrderServiceImpl(repository, mock(PricingPolicy.class));

        ConcurrentModificationException exception = assertThrows(
                ConcurrentModificationException.class, () -> service.persistChanges(order));
        assertEquals("Repair order 'null' was modified concurrently. Reload it and retry.",
                exception.getMessage());
    }
}
