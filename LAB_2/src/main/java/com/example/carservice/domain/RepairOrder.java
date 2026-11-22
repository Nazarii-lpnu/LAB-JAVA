package com.example.carservice.domain;

import com.example.carservice.exception.EntityNotFoundException;
import com.example.carservice.exception.InvalidArgumentException;
import com.example.carservice.exception.InvalidStateTransitionException;
import com.example.carservice.exception.MechanicNotCompatibleException;
import com.example.carservice.exception.OrderModificationNotAllowedException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RepairOrder {
    private static int MONEY_SCALE = 2;
    private UUID id;
    private Car car;
    private LocalDateTime createdAt;
    private OrderPriority priority;
    private List<ServiceWork> works = new ArrayList<>();
    private Mechanic mechanic;
    private OrderStatus status = OrderStatus.CREATED;
    private TimeSlot plannedSlot;

    public RepairOrder(UUID id, Car car) {
        this(id, car, OrderPriority.NORMAL, LocalDateTime.now());
    }

    public RepairOrder(UUID id, Car car, OrderPriority priority) {
        this(id, car, priority, LocalDateTime.now());
    }

    public RepairOrder(UUID id, Car car, OrderPriority priority, LocalDateTime createdAt) {
        if (id == null) throw new InvalidArgumentException("Order id cannot be null.");
        if (car == null) throw new InvalidArgumentException("Order car cannot be null.");
        if (priority == null) throw new InvalidArgumentException("Order priority cannot be null.");
        if (createdAt == null) throw new InvalidArgumentException("Order creation time cannot be null.");
        this.id = id;
        this.car = car;
        this.priority = priority;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public Car getCar() { return car; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public OrderPriority getPriority() { return priority; }
    public Mechanic getMechanic() { return mechanic; }
    public OrderStatus getStatus() { return status; }
    public TimeSlot getPlannedSlot() { return plannedSlot; }
    public List<ServiceWork> getWorks() { return List.copyOf(works); }

    public Optional<Gift> getGift() {
        return Gift.forOrderAmount(calculateBaseCost());
    }

    public BigDecimal calculateBaseCost() {
        BigDecimal parts = works.stream().map(ServiceWork::getPartsCost).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal labor = works.stream().map(ServiceWork::getLaborCost).reduce(BigDecimal.ZERO, BigDecimal::add);
        return parts.add(labor).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public void addWork(ServiceWork work) {
        ensureModifiable();
        if (work == null) throw new InvalidArgumentException("Service work cannot be null.");
        if (works.stream().anyMatch(existing -> existing.getId().equals(work.getId()))) {
            throw new InvalidArgumentException("A work with id '" + work.getId() + "' is already in the order.");
        }
        ensureMechanicCanPerform(work);
        works.add(work);
    }

    public void removeWork(UUID workId) {
        ensureModifiable();
        ServiceWork work = findWork(workId);
        works.remove(work);
    }

    public void assignMechanic(Mechanic mechanic) {
        ensureModifiable();
        if (mechanic == null) throw new InvalidArgumentException("Mechanic cannot be null.");
        if (!mechanic.canPerformAll(works)) {
            throw new MechanicNotCompatibleException("Mechanic '" + mechanic.getId() + "' is not compatible with every work in the order.");
        }
        this.mechanic = mechanic;
    }

    public void plan(TimeSlot timeSlot) {
        ensureModifiable();
        if (timeSlot == null) throw new InvalidArgumentException("Planned time slot cannot be null.");
        plannedSlot = timeSlot;
    }

    public void diagnose() { transitionTo(OrderStatus.DIAGNOSED); }

    public void approve() {
        ensureTransitionAllowed(OrderStatus.APPROVED);
        if (works.isEmpty()) throw new InvalidStateTransitionException("An order cannot be approved without at least one work.");
        status = OrderStatus.APPROVED;
    }

    public void start() {
        ensureTransitionAllowed(OrderStatus.IN_PROGRESS);
        if (mechanic == null) throw new InvalidStateTransitionException("An order cannot start without an assigned mechanic.");
        status = OrderStatus.IN_PROGRESS;
    }

    public void completeWork(UUID workId) {
        if (status != OrderStatus.IN_PROGRESS) throw new InvalidStateTransitionException("Work can be completed only while the order is in progress.");
        findWork(workId).complete();
    }

    public void complete() {
        if (status != OrderStatus.IN_PROGRESS) throw new InvalidStateTransitionException("An order can be completed only while it is in progress.");
        if (works.stream().anyMatch(work -> !work.isCompleted())) {
            throw new InvalidStateTransitionException("An order cannot be completed while unfinished work remains.");
        }
        status = OrderStatus.COMPLETED;
    }

    public void cancel() { transitionTo(OrderStatus.CANCELLED); }

    private void ensureMechanicCanPerform(ServiceWork work) {
        if (mechanic != null && !mechanic.canPerform(work)) {
            throw new MechanicNotCompatibleException("Assigned mechanic '" + mechanic.getId() + "' is not compatible with work '" + work.getId() + "'.");
        }
    }

    private ServiceWork findWork(UUID workId) {
        if (workId == null) throw new InvalidArgumentException("Work id cannot be null.");
        return works.stream().filter(work -> work.getId().equals(workId)).findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Work with id '" + workId + "' is not part of this order."));
    }

    private void ensureModifiable() {
        if (status == OrderStatus.COMPLETED || status == OrderStatus.CANCELLED) {
            throw new OrderModificationNotAllowedException("Completed or cancelled orders cannot be modified.");
        }
    }

    private void transitionTo(OrderStatus target) {
        ensureTransitionAllowed(target);
        status = target;
    }

    private void ensureTransitionAllowed(OrderStatus target) {
        if (!isAllowedTransition(target)) {
            throw new InvalidStateTransitionException("Cannot change order status from " + status + " to " + target + ".");
        }
    }

    private boolean isAllowedTransition(OrderStatus target) {
        return (status == OrderStatus.CREATED && (target == OrderStatus.DIAGNOSED || target == OrderStatus.CANCELLED))
                || (status == OrderStatus.DIAGNOSED && (target == OrderStatus.APPROVED || target == OrderStatus.CANCELLED))
                || (status == OrderStatus.APPROVED && (target == OrderStatus.IN_PROGRESS || target == OrderStatus.CANCELLED))
                || (status == OrderStatus.IN_PROGRESS && target == OrderStatus.COMPLETED);
    }
}
