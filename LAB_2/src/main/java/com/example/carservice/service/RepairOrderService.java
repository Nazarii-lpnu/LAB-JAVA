package com.example.carservice.service;

import com.example.carservice.domain.Mechanic;
import com.example.carservice.domain.RepairOrder;
import com.example.carservice.domain.ServiceWork;
import com.example.carservice.domain.TimeSlot;
import com.example.carservice.exception.EntityNotFoundException;
import com.example.carservice.exception.InvalidArgumentException;
import com.example.carservice.exception.MechanicAlreadyAssignedException;
import com.example.carservice.exception.ScheduleConflictException;
import com.example.carservice.repository.RepairOrderRepository;
import com.example.carservice.pricing.CommercialPricingPolicy;
import com.example.carservice.pricing.PricingPolicy;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class RepairOrderService {
    private RepairOrderRepository repository;
    private PricingPolicy pricingPolicy;

    public RepairOrderService(RepairOrderRepository repository) {
        this(repository, new CommercialPricingPolicy());
    }

    public RepairOrderService(RepairOrderRepository repository, PricingPolicy pricingPolicy) {
        this.repository = repository;
        if (pricingPolicy == null) throw new InvalidArgumentException("Pricing policy cannot be null.");
        this.pricingPolicy = pricingPolicy;
    }

    public RepairOrder create(RepairOrder order) { return repository.save(order); }

    public BigDecimal calculateBasePrice(UUID orderId) {
        return getById(orderId).calculateBaseCost();
    }

    public BigDecimal calculateCommercialPrice(UUID orderId) {
        RepairOrder order = getById(orderId);
        BigDecimal price = pricingPolicy.calculatePrice(order);
        repository.update(order);
        return price;
    }

    public RepairOrder getById(UUID orderId) {
        return repository.findById(orderId).orElseThrow(() ->
                new EntityNotFoundException("No service order was found for id '" + orderId + "'."));
    }

    public void addWork(UUID orderId, ServiceWork work) { update(getById(orderId), order -> order.addWork(work)); }
    public void removeWork(UUID orderId, UUID workId) { update(getById(orderId), order -> order.removeWork(workId)); }
    public void diagnose(UUID orderId) { update(getById(orderId), RepairOrder::diagnose); }
    public void approve(UUID orderId) { update(getById(orderId), RepairOrder::approve); }
    public void start(UUID orderId) {
        RepairOrder order = getById(orderId);
        ensureMechanicAvailable(order, order.getMechanic());
        update(order, RepairOrder::start);
    }
    public void completeWork(UUID orderId, UUID workId) { update(getById(orderId), order -> order.completeWork(workId)); }
    public void complete(UUID orderId) { update(getById(orderId), RepairOrder::complete); }
    public void cancel(UUID orderId) { update(getById(orderId), RepairOrder::cancel); }

    public void assignMechanic(UUID orderId, Mechanic mechanic) {
        RepairOrder order = getById(orderId);
        ensureMechanicAvailable(order, mechanic);
        update(order, current -> current.assignMechanic(mechanic));
    }

    public void plan(UUID orderId, LocalDateTime start, Duration duration) {
        RepairOrder order = getById(orderId);
        TimeSlot slot = new TimeSlot(start, duration);
        ensureScheduleAvailable(order, slot, order.getMechanic());
        update(order, current -> current.plan(slot));
    }

    public List<RepairOrder> findByCarVin(String vin) { return repository.findByCarVin(vin); }
    public List<RepairOrder> findByMechanic(Mechanic mechanic) { return repository.findByMechanic(mechanic); }

    private void update(RepairOrder order, java.util.function.Consumer<RepairOrder> operation) {
        operation.accept(order);
        repository.update(order);
    }

    private void ensureMechanicAvailable(RepairOrder target, Mechanic mechanic) {
        if (mechanic == null) return;
        List<RepairOrder> others = repository.findByMechanic(mechanic).stream()
                .filter(order -> !order.equals(target))
                .filter(order -> order.getStatus() != com.example.carservice.domain.OrderStatus.COMPLETED
                        && order.getStatus() != com.example.carservice.domain.OrderStatus.CANCELLED)
                .toList();
        for (RepairOrder other : others) {
            if (target.getPlannedSlot() == null || other.getPlannedSlot() == null) {
                throw new MechanicAlreadyAssignedException("Mechanic '" + mechanic.getId()
                        + "' is already assigned to another active order without enough scheduling information to prove availability.");
            }
        }
        if (target.getPlannedSlot() != null) ensureScheduleAvailable(target, target.getPlannedSlot(), mechanic);
    }

    private void ensureScheduleAvailable(RepairOrder target, TimeSlot slot, Mechanic mechanic) {
        if (mechanic == null) return;
        boolean conflict = repository.findByMechanic(mechanic).stream()
                .filter(order -> !order.equals(target))
                .filter(order -> order.getStatus() != com.example.carservice.domain.OrderStatus.COMPLETED
                        && order.getStatus() != com.example.carservice.domain.OrderStatus.CANCELLED)
                .peek(order -> {
                    if (order.getPlannedSlot() == null) {
                        throw new MechanicAlreadyAssignedException("Mechanic '" + mechanic.getId()
                                + "' is already assigned to another active order without a planned slot.");
                    }
                })
                .anyMatch(order -> order.getPlannedSlot().overlaps(slot));
        if (conflict) throw new ScheduleConflictException("The planned time slot conflicts with another order for mechanic '" + mechanic.getId() + "'.");
    }
}
