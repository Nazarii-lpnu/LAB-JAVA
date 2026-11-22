package com.example.carservice.service;

import com.example.carservice.TestFixtures;
import com.example.carservice.domain.Mechanic;
import com.example.carservice.domain.enumeration.MechanicSpecialization;
import com.example.carservice.domain.RepairOrder;
import com.example.carservice.domain.ServiceWork;
import com.example.carservice.domain.enumeration.OrderStatus;
import com.example.carservice.exception.EntityNotFoundException;
import com.example.carservice.exception.InvalidStateTransitionException;
import com.example.carservice.exception.MechanicNotCompatibleException;
import com.example.carservice.pricing.CommercialPricingPolicy;
import com.example.carservice.pricing.PricingPolicy;
import com.example.carservice.pricing.PricingRuleEngine;
import com.example.carservice.repository.RepairOrderRepository;
import com.example.carservice.service.impl.RepairOrderServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RepairOrderServiceComprehensiveTest {
    @Test
    void createsOrderThroughRepository() {
        RepairOrderRepository repository = mock(RepairOrderRepository.class);
        RepairOrder order = TestFixtures.order();
        when(repository.saveAndFlush(order)).thenReturn(order);
        RepairOrderService service = service(repository);

        assertEquals(order, service.create(order));
        verify(repository).saveAndFlush(order);
    }

    @Test
    void retrievesExistingOrder() {
        RepairOrderRepository repository = mock(RepairOrderRepository.class);
        RepairOrder order = TestFixtures.order();
        when(repository.findById(orderId(order))).thenReturn(Optional.of(order));

        assertEquals(order, service(repository).getById(orderId(order)));
        verify(repository).findById(orderId(order));
    }

    @Test
    void translatesMissingOrderIntoDomainException() {
        RepairOrderRepository repository = mock(RepairOrderRepository.class);
        long id = 999L;
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service(repository).getById(id));
    }

    @Test
    void addsWorkAndUpdatesRepository() {
        RepairOrderRepository repository = mock(RepairOrderRepository.class);
        RepairOrder order = TestFixtures.order();
        when(repository.findById(orderId(order))).thenReturn(Optional.of(order));
        RepairOrderService service = service(repository);
        ServiceWork work = TestFixtures.work();

        service.addWork(orderId(order), work);

        assertEquals(List.of(work), order.getWorks());
        verify(repository).saveAndFlush(order);
    }

    @Test
    void removesWorkAndUpdatesRepository() {
        RepairOrderRepository repository = mock(RepairOrderRepository.class);
        RepairOrder order = TestFixtures.order();
        ServiceWork work = TestFixtures.work();
        order.addWork(work);
        when(repository.findById(orderId(order))).thenReturn(Optional.of(order));

        service(repository).removeWork(orderId(order), workId(work));

        assertEquals(List.of(), order.getWorks());
        verify(repository).saveAndFlush(order);
    }

    @Test
    void invalidDomainOperationDoesNotUpdateRepository() {
        RepairOrderRepository repository = mock(RepairOrderRepository.class);
        RepairOrder order = TestFixtures.order();
        when(repository.findById(orderId(order))).thenReturn(Optional.of(order));

        assertThrows(InvalidStateTransitionException.class,
                () -> service(repository).approve(orderId(order)));

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void calculatesBasePriceFromRetrievedOrder() {
        RepairOrderRepository repository = mock(RepairOrderRepository.class);
        RepairOrder order = TestFixtures.order();
        order.addWork(TestFixtures.work(MechanicSpecialization.ENGINE, "12.00", "8.00", 1));
        when(repository.findById(orderId(order))).thenReturn(Optional.of(order));

        assertEquals(new BigDecimal("20.00"),
                service(repository).calculateBasePrice(orderId(order)));
    }

    @Test
    void delegatesCommercialPriceToInjectedPolicyAndPersistsOrder() {
        RepairOrderRepository repository = mock(RepairOrderRepository.class);
        PricingPolicy policy = mock(PricingPolicy.class);
        RepairOrder order = TestFixtures.order();
        when(repository.findById(orderId(order))).thenReturn(Optional.of(order));
        when(policy.calculatePrice(order)).thenReturn(new BigDecimal("19.99"));

        assertEquals(new BigDecimal("19.99"),
                new RepairOrderServiceImpl(repository, policy).calculateCommercialPrice(orderId(order)));
        verify(policy).calculatePrice(order);
        verify(repository).saveAndFlush(order);
    }

    @Test
    void doesNotPersistWhenPricingPolicyFails() {
        RepairOrderRepository repository = mock(RepairOrderRepository.class);
        PricingPolicy policy = mock(PricingPolicy.class);
        RepairOrder order = TestFixtures.order();
        when(repository.findById(orderId(order))).thenReturn(Optional.of(order));
        when(policy.calculatePrice(order)).thenThrow(new IllegalStateException("pricing failed"));

        assertThrows(IllegalStateException.class,
                () -> new RepairOrderServiceImpl(repository, policy).calculateCommercialPrice(orderId(order)));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void diagnosesAndApprovesOrderThroughService() {
        RepairOrderRepository repository = mock(RepairOrderRepository.class);
        RepairOrder order = TestFixtures.order();
        order.addWork(TestFixtures.work());
        when(repository.findById(orderId(order))).thenReturn(Optional.of(order));

        RepairOrderService service = service(repository);
        service.diagnose(orderId(order));
        service.approve(orderId(order));

        assertEquals(OrderStatus.APPROVED, order.getStatus());
        verify(repository, org.mockito.Mockito.times(2)).saveAndFlush(order);
    }

    @Test
    void assignsCompatibleMechanicAndPersists() {
        RepairOrderRepository repository = mock(RepairOrderRepository.class);
        RepairOrder order = TestFixtures.order();
        order.addWork(TestFixtures.work(MechanicSpecialization.ENGINE, "1.00", "1.00", 1));
        Mechanic mechanic = new Mechanic("Engine", MechanicSpecialization.ENGINE);
        when(repository.findById(orderId(order))).thenReturn(Optional.of(order));
        when(repository.findDetailedByMechanic(mechanic)).thenReturn(List.of());

        service(repository).assignMechanic(orderId(order), mechanic);

        assertEquals(mechanic, order.getMechanic());
        verify(repository).saveAndFlush(order);
    }

    @Test
    void rejectsIncompatibleMechanicWithoutPersisting() {
        RepairOrderRepository repository = mock(RepairOrderRepository.class);
        RepairOrder order = TestFixtures.order();
        order.addWork(TestFixtures.work(MechanicSpecialization.ENGINE, "1.00", "1.00", 1));
        Mechanic mechanic = new Mechanic("Brakes", MechanicSpecialization.BRAKES);
        when(repository.findById(orderId(order))).thenReturn(Optional.of(order));
        when(repository.findDetailedByMechanic(mechanic)).thenReturn(List.of());

        assertThrows(MechanicNotCompatibleException.class,
                () -> service(repository).assignMechanic(orderId(order), mechanic));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void plansOrderAndPersistsSlot() {
        RepairOrderRepository repository = mock(RepairOrderRepository.class);
        RepairOrder order = TestFixtures.order();
        when(repository.findById(orderId(order))).thenReturn(Optional.of(order));

        service(repository).plan(orderId(order),
                Instant.parse("2026-01-01T09:00:00Z"), Duration.ofHours(2));

        assertEquals(Instant.parse("2026-01-01T11:00:00Z"), order.getPlannedSlot().getEnd());
        verify(repository).saveAndFlush(order);
    }

    @Test
    void startsOrderAfterValidPreparation() {
        RepairOrderRepository repository = mock(RepairOrderRepository.class);
        RepairOrder order = TestFixtures.order();
        ServiceWork work = TestFixtures.work();
        Mechanic mechanic = new Mechanic("General", MechanicSpecialization.GENERAL);
        order.addWork(work);
        order.diagnose();
        order.approve();
        order.assignMechanic(mechanic);
        when(repository.findById(orderId(order))).thenReturn(Optional.of(order));
        when(repository.findDetailedByMechanic(mechanic)).thenReturn(List.of(order));

        service(repository).start(orderId(order));

        assertEquals(OrderStatus.IN_PROGRESS, order.getStatus());
        verify(repository).saveAndFlush(order);
    }

    @Test
    void delegatesSearchByCarAndMechanic() {
        RepairOrderRepository repository = mock(RepairOrderRepository.class);
        RepairOrder order = TestFixtures.order();
        Mechanic mechanic = new Mechanic("Mechanic", MechanicSpecialization.GENERAL);
        when(repository.findDetailedByCarVin(order.getCar().getVin())).thenReturn(List.of(order));
        when(repository.findDetailedByMechanic(mechanic)).thenReturn(List.of(order));
        RepairOrderService service = service(repository);

        assertEquals(List.of(order), service.findByCarVin(order.getCar().getVin()));
        assertEquals(List.of(order), service.findByMechanic(mechanic));
        verify(repository).findDetailedByCarVin(order.getCar().getVin());
        verify(repository).findDetailedByMechanic(mechanic);
    }

    @Test
    void completesWorkAndOrderThroughService() {
        RepairOrderRepository repository = mock(RepairOrderRepository.class);
        RepairOrder order = TestFixtures.order();
        ServiceWork work = TestFixtures.work();
        order.addWork(work);
        order.diagnose();
        order.approve();
        order.assignMechanic(new Mechanic("General", MechanicSpecialization.GENERAL));
        order.start();
        when(repository.findById(orderId(order))).thenReturn(Optional.of(order));

        RepairOrderService service = service(repository);
        service.completeWork(orderId(order), workId(work));
        service.complete(orderId(order));

        assertEquals(OrderStatus.COMPLETED, order.getStatus());
        verify(repository, org.mockito.Mockito.times(2)).saveAndFlush(order);
    }

    @Test
    void cancellationIsPersisted() {
        RepairOrderRepository repository = mock(RepairOrderRepository.class);
        RepairOrder order = TestFixtures.order();
        when(repository.findById(orderId(order))).thenReturn(Optional.of(order));

        service(repository).cancel(orderId(order));

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verify(repository).saveAndFlush(order);
    }

    @Test
    void repositoryUpdateFailureIsPropagated() {
        RepairOrderRepository repository = mock(RepairOrderRepository.class);
        RepairOrder order = TestFixtures.order();
        when(repository.findById(orderId(order))).thenReturn(Optional.of(order));
        doThrow(new IllegalStateException("storage failure")).when(repository).saveAndFlush(order);

        assertThrows(IllegalStateException.class,
                () -> service(repository).diagnose(orderId(order)));
    }

    @Test
    void nullPricingPolicyIsRejected() {
        RepairOrderRepository repository = mock(RepairOrderRepository.class);

        assertThrows(com.example.carservice.exception.InvalidArgumentException.class,
                () -> new RepairOrderServiceImpl(repository, null));
    }

    private static RepairOrderService service(RepairOrderRepository repository) {
        return new RepairOrderServiceImpl(repository,
                new CommercialPricingPolicy(new PricingRuleEngine(List.of())));
    }

    private static long orderId(RepairOrder order) {
        return order.getId() == null ? 1L : order.getId();
    }

    private static long workId(ServiceWork work) {
        return work.getId() == null ? 0L : work.getId();
    }
}
