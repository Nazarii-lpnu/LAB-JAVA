package com.example.carservice.domain;

import com.example.carservice.exception.EntityNotFoundException;
import com.example.carservice.exception.InvalidArgumentException;
import com.example.carservice.exception.InvalidStateTransitionException;
import com.example.carservice.exception.MechanicNotCompatibleException;
import com.example.carservice.exception.OrderModificationNotAllowedException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ServiceOrder {

    private static final BigDecimal URGENT_LABOR_MULTIPLIER = new BigDecimal("1.20");
    private static final int MONEY_SCALE = 2;

    private final UUID id;
    private final Car car;
    private final List<ServiceWork> works;
    private final LocalDateTime createdAt;
    private Mechanic mechanic;
    private OrderStatus status;
    private OrderPriority priority;
    private TimeSlot plannedSlot;

    public ServiceOrder(UUID id, Car car) {
        this(id, car, OrderPriority.NORMAL, LocalDateTime.now());
    }

    public ServiceOrder(UUID id, Car car, OrderPriority priority) {
        this(id, car, priority, LocalDateTime.now());
    }

    public ServiceOrder(UUID id, Car car, OrderPriority priority, LocalDateTime createdAt) {
        if (id == null) {
            throw new InvalidArgumentException("Order id cannot be null.");
        }
        if (car == null) {
            throw new InvalidArgumentException("Order car cannot be null.");
        }
        if (priority == null) {
            throw new InvalidArgumentException("Order priority cannot be null.");
        }
        if (createdAt == null) {
            throw new InvalidArgumentException("Order creation time cannot be null.");
        }

        this.id = id;
        this.car = car;
        this.works = new ArrayList<>();
        this.createdAt = createdAt;
        this.status = OrderStatus.CREATED;
        this.priority = priority;
    }

    public UUID getId() {
        return id;
    }

    public Car getCar() {
        return car;
    }

    public List<ServiceWork> getWorks() {
        return List.copyOf(works);
    }

    public Mechanic getMechanic() {
        return mechanic;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public OrderPriority getPriority() {
        return priority;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public TimeSlot getPlannedSlot() {
        return plannedSlot;
    }

    public BigDecimal calculateTotalCost() {
        BigDecimal partsCost = BigDecimal.ZERO;
        BigDecimal laborCost = BigDecimal.ZERO;

        for (ServiceWork work : works) {
            partsCost = partsCost.add(work.getPartsCost());
            laborCost = laborCost.add(work.getLaborCost());
        }

        if (priority == OrderPriority.URGENT) {
            laborCost = laborCost.multiply(URGENT_LABOR_MULTIPLIER);
        }

        return partsCost.add(laborCost).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal getTotalCost() {
        return calculateTotalCost();
    }

    public void addWork(ServiceWork work) {
        ensureModifiable();
        if (work == null) {
            throw new InvalidArgumentException("Service work cannot be null.");
        }
        if (works.stream().anyMatch(existingWork -> existingWork.getId().equals(work.getId()))) {
            throw new InvalidArgumentException("A work with id '" + work.getId() + "' is already in the order.");
        }
        ensureMechanicCanPerform(work);
        works.add(work);
    }

    public void removeWork(UUID workId) {
        ensureModifiable();
        if (workId == null) {
            throw new InvalidArgumentException("Work id cannot be null.");
        }

        ServiceWork work = findWork(workId);
        works.remove(work);
    }

    public void assignMechanic(Mechanic mechanic) {
        ensureModifiable();
        if (mechanic == null) {
            throw new InvalidArgumentException("Mechanic cannot be null.");
        }
        if (!mechanic.canPerformAll(works)) {
            throw new MechanicNotCompatibleException(
                    "Mechanic '" + mechanic.getId()
                            + "' is not compatible with every work in the order.");
        }

        this.mechanic = mechanic;
    }

    public void planStartAt(LocalDateTime start, Duration duration) {
        ensureModifiable();
        this.plannedSlot = new TimeSlot(start, duration);
    }

    public void plan(TimeSlot timeSlot) {
        ensureModifiable();
        if (timeSlot == null) {
            throw new InvalidArgumentException("Planned time slot cannot be null.");
        }
        this.plannedSlot = timeSlot;
    }

    public void diagnose() {
        transitionTo(OrderStatus.DIAGNOSED);
    }

    public void approve() {
        ensureTransitionAllowed(OrderStatus.APPROVED);
        if (works.isEmpty()) {
            throw new InvalidStateTransitionException("An order cannot be approved without at least one work.");
        }
        status = OrderStatus.APPROVED;
    }

    public void start() {
        ensureTransitionAllowed(OrderStatus.IN_PROGRESS);
        if (mechanic == null) {
            throw new InvalidStateTransitionException("An order cannot start without an assigned mechanic.");
        }
        status = OrderStatus.IN_PROGRESS;
    }

    public void completeWork(UUID workId) {
        if (status != OrderStatus.IN_PROGRESS) {
            throw new InvalidStateTransitionException(
                    "Work can be completed only while the order is in progress.");
        }
        ServiceWork work = findWork(workId);
        work.complete();
    }

    public void complete() {
        if (status != OrderStatus.IN_PROGRESS) {
            throw new InvalidStateTransitionException(
                    "An order can be completed only while it is in progress.");
        }
        if (works.stream().anyMatch(work -> !work.isCompleted())) {
            throw new InvalidStateTransitionException(
                    "An order cannot be completed while unfinished work remains.");
        }
        status = OrderStatus.COMPLETED;
    }

    public void cancel() {
        transitionTo(OrderStatus.CANCELLED);
    }

    private void ensureTransitionAllowed(OrderStatus targetStatus) {
        if (!isAllowedTransition(status, targetStatus)) {
            throw new InvalidStateTransitionException(
                    "Cannot change order status from " + status + " to " + targetStatus + ".");
        }
    }

    private void transitionTo(OrderStatus targetStatus) {
        ensureTransitionAllowed(targetStatus);
        status = targetStatus;
    }

    private boolean isAllowedTransition(OrderStatus currentStatus, OrderStatus targetStatus) {
        return (currentStatus == OrderStatus.CREATED && targetStatus == OrderStatus.DIAGNOSED)
                || (currentStatus == OrderStatus.CREATED && targetStatus == OrderStatus.CANCELLED)
                || (currentStatus == OrderStatus.DIAGNOSED && targetStatus == OrderStatus.APPROVED)
                || (currentStatus == OrderStatus.DIAGNOSED && targetStatus == OrderStatus.CANCELLED)
                || (currentStatus == OrderStatus.APPROVED && targetStatus == OrderStatus.IN_PROGRESS)
                || (currentStatus == OrderStatus.APPROVED && targetStatus == OrderStatus.CANCELLED)
                || (currentStatus == OrderStatus.IN_PROGRESS && targetStatus == OrderStatus.COMPLETED);
    }

    private void ensureModifiable() {
        if (status == OrderStatus.COMPLETED || status == OrderStatus.CANCELLED) {
            throw new OrderModificationNotAllowedException(
                    "Completed or cancelled orders cannot be modified.");
        }
    }

    private void ensureMechanicCanPerform(ServiceWork work) {
        if (mechanic != null && !mechanic.canPerformAll(List.of(work))) {
            throw new MechanicNotCompatibleException(
                    "Assigned mechanic '" + mechanic.getId()
                            + "' is not compatible with work '" + work.getId() + "'.");
        }
    }

    private ServiceWork findWork(UUID workId) {
        if (workId == null) {
            throw new InvalidArgumentException("Work id cannot be null.");
        }

        return works.stream()
                .filter(work -> work.getId().equals(workId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                        "Work with id '" + workId + "' is not part of this order."));
    }
}
