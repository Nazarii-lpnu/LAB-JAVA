package com.example.carservice.service.impl;

import com.example.carservice.domain.Mechanic;
import com.example.carservice.domain.RepairOrder;
import com.example.carservice.domain.ServiceWork;
import com.example.carservice.domain.TimeSlot;
import com.example.carservice.domain.enumeration.OrderStatus;
import com.example.carservice.exception.ConcurrentModificationException;
import com.example.carservice.exception.EntityAlreadyExistsException;
import com.example.carservice.exception.EntityNotFoundException;
import com.example.carservice.exception.InvalidArgumentException;
import com.example.carservice.exception.MechanicAlreadyAssignedException;
import com.example.carservice.exception.ScheduleConflictException;
import com.example.carservice.pricing.PricingPolicy;
import com.example.carservice.repository.RepairOrderRepository;
import com.example.carservice.service.RepairOrderService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@Service
@Transactional
public class RepairOrderServiceImpl implements RepairOrderService {
    private final RepairOrderRepository repository;
    private final PricingPolicy pricingPolicy;

    public RepairOrderServiceImpl(RepairOrderRepository repository, PricingPolicy pricingPolicy) {
        this.repository = repository;
        if (Objects.isNull(pricingPolicy))
            throw new InvalidArgumentException("Pricing policy cannot be null.");
        this.pricingPolicy = pricingPolicy;
    }

    @Override
    public RepairOrder create(RepairOrder order) {
        return saveNewOrder(order);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateBasePrice(long orderId) {
        return loadOrder(orderId).calculateBaseCost();
    }

    @Override
    public BigDecimal calculateCommercialPrice(long orderId) {
        RepairOrder order = loadOrder(orderId);
        BigDecimal price = pricingPolicy.calculatePrice(order);
        order.recordCommercialPrice(price);
        persistChanges(order);
        return price;
    }

    @Override
    @Transactional(readOnly = true)
    public RepairOrder getById(long orderId) {
        return loadOrder(orderId);
    }

    @Override
    public void addWork(long orderId, ServiceWork work) {
        update(loadOrder(orderId), order -> order.addWork(work));
    }

    @Override
    @Transactional
    public void addWorks(long orderId, Collection<ServiceWork> works) {
        if (Objects.isNull(works)) throw new InvalidArgumentException("Service works cannot be null.");
        RepairOrder order = loadOrder(orderId);
        works.forEach(order::addWork);
        persistChanges(order);
    }

    @Override
    public void removeWork(long orderId, long workId) {
        update(loadOrder(orderId), order -> order.removeWork(workId));
    }

    @Override
    public void diagnose(long orderId) {
        update(loadOrder(orderId), RepairOrder::diagnose);
    }

    @Override
    public void approve(long orderId) {
        update(loadOrder(orderId), RepairOrder::approve);
    }

    @Override
    public void start(long orderId) {
        RepairOrder order = loadOrder(orderId);
        ensureMechanicAvailable(order, order.getMechanic());
        update(order, RepairOrder::start);
    }

    @Override
    public void completeWork(long orderId, long workId) {
        update(loadOrder(orderId), order -> order.completeWork(workId));
    }

    @Override
    @Transactional
    public void complete(long orderId) {
        RepairOrder order = loadOrder(orderId);
        order.complete();
        BigDecimal price = pricingPolicy.calculatePrice(order);
        order.recordCommercialPrice(price);
        persistChanges(order);
    }

    @Override
    public void cancel(long orderId) {
        update(loadOrder(orderId), RepairOrder::cancel);
    }

    @Override
    public void assignMechanic(long orderId, Mechanic mechanic) {
        RepairOrder order = loadOrder(orderId);
        ensureMechanicAvailable(order, mechanic);
        update(order, current -> current.assignMechanic(mechanic));
    }

    @Override
    public void plan(long orderId, Instant start, Duration duration) {
        RepairOrder order = loadOrder(orderId);
        TimeSlot slot = new TimeSlot(start, duration);
        ensureScheduleAvailable(order, slot, order.getMechanic());
        update(order, current -> current.plan(slot));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RepairOrder> findByCarVin(String vin) {
        return repository.findDetailedByCarVin(vin);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RepairOrder> findByMechanic(Mechanic mechanic) {
        return repository.findDetailedByMechanic(mechanic);
    }

    @Override
    public void persistChanges(RepairOrder order) {
        if (Objects.isNull(order)) throw new InvalidArgumentException("Repair order cannot be null.");
        try {
            repository.saveAndFlush(order);
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new ConcurrentModificationException(
                    "Repair order '" + order.getId() + "' was modified concurrently. Reload it and retry.",
                    exception);
        }
    }

    private RepairOrder loadOrder(long orderId) {
        return repository.findDetailedById(orderId)
                .or(() -> repository.findById(orderId))
                .orElseThrow(() -> new EntityNotFoundException(
                        "No service order was found for id '" + orderId + "'."));
    }

    private void update(RepairOrder order, Consumer<RepairOrder> operation) {
        operation.accept(order);
        persistChanges(order);
    }

    private void ensureMechanicAvailable(RepairOrder target, Mechanic mechanic) {
        if (Objects.isNull(mechanic)) return;
        List<RepairOrder> others = repository.findDetailedByMechanic(mechanic).stream()
                .filter(order -> !Objects.equals(order.getId(), target.getId()))
                .filter(order -> !Objects.equals(order.getStatus(), OrderStatus.COMPLETED)
                        && !Objects.equals(order.getStatus(), OrderStatus.CANCELLED))
                .toList();
        if (others.stream().anyMatch(other -> Objects.isNull(target.getPlannedSlot()) || Objects.isNull(other.getPlannedSlot()))) {
            throw new MechanicAlreadyAssignedException("Mechanic '" + mechanic.getId()
                    + "' is already assigned to another active order without enough scheduling information to prove availability.");
        }
        if (Objects.nonNull(target.getPlannedSlot()))
            ensureScheduleAvailable(target, target.getPlannedSlot(), mechanic);
    }

    private void ensureScheduleAvailable(RepairOrder target, TimeSlot slot, Mechanic mechanic) {
        if (Objects.isNull(mechanic)) return;
        List<RepairOrder> others = repository.findDetailedByMechanic(mechanic).stream()
                .filter(order -> !Objects.equals(order.getId(), target.getId()))
                .filter(order -> !Objects.equals(order.getStatus(), OrderStatus.COMPLETED)
                        && !Objects.equals(order.getStatus(), OrderStatus.CANCELLED))
                .toList();
        if (others.stream().anyMatch(order -> Objects.isNull(order.getPlannedSlot()))) {
            throw new MechanicAlreadyAssignedException("Mechanic '" + mechanic.getId()
                    + "' is already assigned to another active order without a planned slot.");
        }
        if (others.stream().anyMatch(order -> order.getPlannedSlot().overlaps(slot))) {
            throw new ScheduleConflictException("The planned time slot conflicts with another order for mechanic '" + mechanic.getId() + "'.");
        }
    }

    private RepairOrder saveNewOrder(RepairOrder order) {
        try {
            return repository.saveAndFlush(order);
        } catch (DataIntegrityViolationException exception) {
            throw new EntityAlreadyExistsException(
                    "A repair order with id '" + order.getId() + "' already exists.");
        }
    }
}
